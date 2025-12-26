package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ParkingDetectionRequest;
import org.example.backend.DTO.ParkingDetectionResponse;
import org.example.backend.entities.ParkingSession;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.entities.Reservation;
import org.example.backend.enums.SessionStatus;
import org.example.backend.repository.ParkingSessionRepository;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingDetectionService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;
    private final ZoneRateService zoneRateService;

    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("10.00");
    private static final BigDecimal MINIMUM_PARKING_FEE = new BigDecimal("5.00");

    private static final boolean SPOT_FREE = true;
    private static final boolean SPOT_OCCUPIED = false;

    @Transactional
    public ParkingDetectionResponse handleDetection(ParkingDetectionRequest request) {

        log.info("=== DÉTECTION REÇUE === SensorId={}, Status={}, Timestamp={}",
                request.getSensorId(), request.getStatus(), request.getTimestamp());

        try {
            ParkingSpot spot = spotRepository.findBySensorId(request.getSensorId())
                    .orElseThrow(() -> new RuntimeException("Capteur non trouvé: " + request.getSensorId()));

            if ("occupied".equalsIgnoreCase(request.getStatus())) {
                return handleCarEntry(spot, request.getTimestamp());
            } else if ("free".equalsIgnoreCase(request.getStatus())) {
                return handleCarExit(spot, request.getTimestamp());
            }

            throw new IllegalArgumentException("Statut invalide: " + request.getStatus());

        } catch (Exception e) {
            log.error("❌ Erreur dans handleDetection: ", e);
            return ParkingDetectionResponse.builder()
                    .action("error")
                    .message("Erreur interne: " + e.getMessage())
                    .build();
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isBlank()) return LocalDateTime.now();

            if (timestamp.endsWith("Z") || timestamp.contains("+")) {
                return OffsetDateTime.parse(timestamp).toLocalDateTime();
            }

            if (timestamp.contains("T") && timestamp.contains(".")) {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            if (timestamp.contains("T")) {
                return LocalDateTime.parse(timestamp);
            }

            String datePart = timestamp.split(" ")[0];
            LocalDateTime now = LocalDateTime.now();
            return LocalDateTime.parse(datePart + "T" +
                    String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));

        } catch (DateTimeParseException e) {
            log.warn("⚠️ Timestamp non reconnu: {}", timestamp);
            return LocalDateTime.now();
        }
    }

    // ========================================================================
    // ✅ HANDLE CAR ENTRY
    // ========================================================================
    private ParkingDetectionResponse handleCarEntry(ParkingSpot spot, String timestamp) {

        log.info("🚗 ENTREE détectée spot {}", spot.getSpotNumber());
        LocalDateTime detectionTime = parseTimestamp(timestamp);

        // 1) Si spot occupé mais pas de session : corriger
        if (spot.getStatus() == SPOT_OCCUPIED) {
            Optional<ParkingSession> active = sessionRepository.findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);
            if (active.isPresent()) {
                ParkingSession existing = active.get();
                boolean hasReservation = existing.getDriverId() != null && !"anonymous".equals(existing.getDriverId());
                return buildEntryResponse(spot, existing, hasReservation, "Spot déjà occupé (session existante)");
            }

            // Incohérence : spot occupé sans session
            spot.setStatus(SPOT_FREE);
            spotRepository.save(spot);
            spotRepository.flush();
        }

        // 2) Vérifier qu’il n’y a pas déjà une session active
        Optional<ParkingSession> activeSession = sessionRepository.findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);
        if (activeSession.isPresent()) {
            ParkingSession session = activeSession.get();
            boolean hasReservation = session.getDriverId() != null && !"anonymous".equals(session.getDriverId());
            return buildEntryResponse(spot, session, hasReservation, "Session déjà active");
        }

        // 3) Chercher réservation CONFIRMED puis PENDING
        Reservation reservation = null;

        List<Reservation> confirmed = reservationRepository.findConfirmedReservationsForSpotAtTime(spot.getId(), detectionTime);
        if (!confirmed.isEmpty()) {
            reservation = confirmed.get(0);
        } else {
            List<Reservation> pending = reservationRepository.findPendingReservationsForSpotAtTime(spot.getId(), detectionTime);
            if (!pending.isEmpty()) {
                reservation = pending.get(0);
            }
        }

        boolean hasReservation = reservation != null;

        // 4) Si réservation trouvée : passer ACTIVE
        String driverId = "anonymous";

        if (reservation != null) {
            log.info("✅ Réservation trouvée ID={} statut={}", reservation.getId(), reservation.getStatus());

            reservation.setStatus("ACTIVE");
            reservationRepository.save(reservation);
            reservationRepository.flush();

            log.info("✅ Réservation {} passée à ACTIVE", reservation.getId());

            if (reservation.getDriverId() != null && !reservation.getDriverId().isBlank()) {
                driverId = reservation.getDriverId();
            }
        }

        // 5) Créer session
        ParkingSession session = ParkingSession.builder()
                .spot(spot)
                .driverId(driverId)
                .startTime(detectionTime)
                .status(SessionStatus.ACTIVE)
                .totalCost(BigDecimal.ZERO)
                .build();

        ParkingSession savedSession = sessionRepository.save(session);
        sessionRepository.flush();

        // 6) Mettre spot OCCUPÉ
        spot.setStatus(SPOT_OCCUPIED);
        spotRepository.save(spot);
        spotRepository.flush();

        String msg = hasReservation ? "Entrée avec réservation → ACTIVE" : "Entrée sans réservation";
        return buildEntryResponse(spot, savedSession, hasReservation, msg);
    }

    // ========================================================================
    // ✅ HANDLE CAR EXIT
    // ========================================================================
    private ParkingDetectionResponse handleCarExit(ParkingSpot spot, String timestamp) {

        log.info("🚪 SORTIE détectée spot {}", spot.getSpotNumber());

        Optional<ParkingSession> activeSession = sessionRepository.findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

        if (activeSession.isEmpty()) {
            if (spot.getStatus() == SPOT_OCCUPIED) {
                spot.setStatus(SPOT_FREE);
                spotRepository.save(spot);
                spotRepository.flush();
                return ParkingDetectionResponse.builder()
                        .action("exit_corrected")
                        .spotId(spot.getId())
                        .spotNumber(spot.getSpotNumber())
                        .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                        .message("Spot libéré (pas de session active)")
                        .build();
            }

            return ParkingDetectionResponse.builder()
                    .action("exit_ignored")
                    .spotId(spot.getId())
                    .spotNumber(spot.getSpotNumber())
                    .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                    .message("Aucune session active")
                    .build();
        }

        ParkingSession session = activeSession.get();
        LocalDateTime exitTime = parseTimestamp(timestamp);

        if (exitTime.isBefore(session.getStartTime())) {
            exitTime = LocalDateTime.now();
        }

        Duration duration = Duration.between(session.getStartTime(), exitTime);
        long minutes = Math.max(1, duration.toMinutes());
        double hours = Math.ceil(minutes / 60.0);

        BigDecimal hourlyRate = getHourlyRate(spot);
        BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));

        if (totalCost.compareTo(MINIMUM_PARKING_FEE) < 0) totalCost = MINIMUM_PARKING_FEE;
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

        session.setEndTime(exitTime);
        session.setTotalCost(totalCost);
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);
        sessionRepository.flush();

        // si driverId != anonymous => mettre la réservation ACTIVE -> COMPLETED
        boolean hadReservation = session.getDriverId() != null && !"anonymous".equals(session.getDriverId());
        if (hadReservation) updateReservationAfterExit(session.getDriverId(), spot.getId(), exitTime);

        spot.setStatus(SPOT_FREE);
        spotRepository.save(spot);
        spotRepository.flush();

        return buildExitResponse(spot, session, minutes, hourlyRate, totalCost, hadReservation, "Sortie enregistrée");
    }

    private void updateReservationAfterExit(String driverId, Long spotId, LocalDateTime exitTime) {

        Optional<Reservation> activeReservationOpt =
                reservationRepository.findActiveReservationForDriverAndSpot(driverId, spotId);

        if (activeReservationOpt.isEmpty()) {
            log.warn("⚠️ No ACTIVE reservation found for driver={} spot={}", driverId, spotId);
            return;
        }

        Reservation reservation = activeReservationOpt.get();

        log.info("✅ ACTIVE reservation found: ID={} status={}", reservation.getId(), reservation.getStatus());

        // ✅ Passer à COMPLETED
        reservation.setStatus("COMPLETED");

        // optionnel : ajuster endTime si la voiture sort après la fin prévue
        if (reservation.getEndTime() != null && reservation.getEndTime().isBefore(exitTime)) {
            reservation.setEndTime(exitTime);
        }

        reservationRepository.save(reservation);
        reservationRepository.flush();

        log.info("✅ Reservation {} updated to COMPLETED", reservation.getId());
    }


    private BigDecimal getHourlyRate(ParkingSpot spot) {
        if (spot.getHourlyRate() != null && spot.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return spot.getHourlyRate();
        }
        return DEFAULT_HOURLY_RATE;
    }

    private ParkingDetectionResponse buildEntryResponse(ParkingSpot spot, ParkingSession session,
                                                        boolean hasReservation, String message) {
        return ParkingDetectionResponse.builder()
                .action("entry_detected")
                .spotId(spot.getId())
                .spotNumber(spot.getSpotNumber())
                .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                .sessionId(session.getId())
                .startTime(session.getStartTime())
                .hasReservation(hasReservation)
                .driverId(session.getDriverId() != null ? session.getDriverId() : "anonymous") // ✅ FIX
                .spotStatus(spot.getStatus() ? "FREE" : "OCCUPIED")
                .message(message)
                .build();
    }

    private ParkingDetectionResponse buildExitResponse(ParkingSpot spot, ParkingSession session,
                                                       long durationMinutes, BigDecimal hourlyRate,
                                                       BigDecimal totalCost, boolean hadReservation,
                                                       String message) {
        return ParkingDetectionResponse.builder()
                .action("exit_detected")
                .spotId(spot.getId())
                .spotNumber(spot.getSpotNumber())
                .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                .sessionId(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .duration(durationMinutes + " minutes")
                .hourlyRate(hourlyRate.doubleValue())
                .totalCost(totalCost.doubleValue())
                .hasReservation(hadReservation)
                .driverId(session.getDriverId() != null ? session.getDriverId() : "anonymous") // ✅ FIX
                .spotStatus(spot.getStatus() ? "FREE" : "OCCUPIED")
                .message(message)
                .build();
    }

    private ParkingDetectionResponse buildErrorResponse(ParkingSpot spot, String errorMessage) {
        return ParkingDetectionResponse.builder()
                .action("error")
                .spotId(spot != null ? spot.getId() : null)
                .spotNumber(spot != null ? spot.getSpotNumber() : null)
                .message(errorMessage)
                .build();
    }
}
