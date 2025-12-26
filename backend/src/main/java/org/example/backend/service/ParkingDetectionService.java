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

    // Configuration tarifaire
    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("10.00");
    private static final BigDecimal MINIMUM_PARKING_FEE = new BigDecimal("5.00");
    private static final BigDecimal NIGHT_RATE_MULTIPLIER = new BigDecimal("0.7");
    private static final BigDecimal WEEKEND_RATE_MULTIPLIER = new BigDecimal("1.2");
    private static final int NIGHT_START_HOUR = 20;
    private static final int NIGHT_END_HOUR = 8;

    // Constantes pour la clarté du code
    private static final boolean SPOT_FREE = false;      // 0 dans la base = LIBRE
    private static final boolean SPOT_OCCUPIED = true;   // 1 dans la base = OCCUPÉ

    @Transactional
    public ParkingDetectionResponse handleDetection(ParkingDetectionRequest request) {
        log.info("=== DÉTECTION REÇUE ===");
        log.info("SensorId: {}, Status: {}, Timestamp: {}",
                request.getSensorId(), request.getStatus(), request.getTimestamp());

        try {
            ParkingSpot spot = spotRepository.findBySensorId(request.getSensorId())
                    .orElseThrow(() -> new RuntimeException("Capteur non trouvé: " + request.getSensorId()));

            log.info("✅ Spot trouvé: ID={}, Numéro={}, Zone={}, Statut actuel={}",
                    spot.getId(), spot.getSpotNumber(),
                    spot.getZone() != null ? spot.getZone().getName() : "N/A",
                    spot.getStatus() ? "OCCUPÉ" : "LIBRE");

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
            if (timestamp.contains("T") && timestamp.contains(".")) {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } else if (timestamp.contains("T")) {
                return LocalDateTime.parse(timestamp);
            } else {
                String datePart = timestamp.split(" ")[0];
                LocalDateTime now = LocalDateTime.now();
                return LocalDateTime.parse(datePart + "T" +
                        String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));
            }
        } catch (DateTimeParseException e) {
            log.warn("Format de timestamp non reconnu: {}, utilisation de l'heure actuelle", timestamp);
            return LocalDateTime.now();
        }
    }

    private ParkingDetectionResponse handleCarEntry(ParkingSpot spot, String timestamp) {
        try {
            log.info("🚗 ENTREE détectée pour le spot {}", spot.getSpotNumber());

            // Vérifier le statut actuel du spot
            if (spot.getStatus() == SPOT_OCCUPIED) {
                log.warn("⚠️ Spot {} déjà occupé - vérification des sessions", spot.getSpotNumber());

                // Vérifier s'il y a une session active
                Optional<ParkingSession> activeSessionOpt = sessionRepository
                        .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

                if (activeSessionOpt.isPresent()) {
                    ParkingSession existingSession = activeSessionOpt.get();
                    log.info("✅ Session active trouvée: {}", existingSession.getId());
                    return buildEntryResponse(spot, existingSession, false, "Spot déjà occupé (session existante)");
                } else {
                    // Spot marqué occupé mais pas de session → corriger l'incohérence
                    log.info("🔧 Correction: Spot marqué occupé mais pas de session → mise à jour du statut");
                    spot.setStatus(SPOT_FREE);
                    spotRepository.save(spot);
                }
            }

            Optional<ParkingSession> activeSessionOpt = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

            if (activeSessionOpt.isPresent()) {
                ParkingSession existingSession = activeSessionOpt.get();
                log.info("Session active déjà présente: {}", existingSession.getId());
                return buildEntryResponse(spot, existingSession, false, "Session déjà active");
            }

            LocalDateTime detectionTime = parseTimestamp(timestamp);

            List<Reservation> activeReservations = reservationRepository
                    .findConfirmedReservationsForSpotAtTime(spot.getId(), detectionTime);
        // Vérifier s'il y a déjà une session active pour ce spot
        ParkingSession existingSession = sessionRepository
                .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE)
                .orElse(null);

        if (existingSession != null) {
            // Spot déjà occupé, retourner les infos de la session existante
            return ParkingDetectionResponse.builder()
                    .action("entry_detected")
                    .spotId(spot.getId())
                    .spotNumber(spot.getSpotNumber())
                    .zoneName(spot.getZone().getName())
                    .sessionId(existingSession.getId())
                    .hasReservation(false)
                    .build();
        }

        // Vérifier s'il y a une réservation PENDING pour ce spot (utilisateur a réservé mais pas encore entré)
        LocalDateTime detectionTime = LocalDateTime.parse(timestamp);
        List<Reservation> pendingReservations = reservationRepository
                .findPendingReservationsForSpotAtTime(spot.getId(), detectionTime);

            boolean hasReservation = !activeReservations.isEmpty();
            Reservation reservation = hasReservation ? activeReservations.get(0) : null;
        boolean hasReservation = !pendingReservations.isEmpty();
        Reservation reservation = hasReservation ? pendingReservations.get(0) : null;

        // IMPORTANT: Ne créer une session QUE si une réservation existe
        if (!hasReservation) {
            throw new RuntimeException("Aucune réservation trouvée pour ce spot. L'utilisateur doit d'abord réserver.");
        }

        // Trouver la session PENDING existante (créée lors de la réservation)
        ParkingSession pendingSession = sessionRepository
                .findBySpotIdAndStatus(spot.getId(), SessionStatus.PENDING)
                .orElse(null);

        ParkingSession activeSession;
        if (pendingSession != null) {
            // Mettre à jour la session PENDING → ACTIVE et démarrer le timer
            pendingSession.setStartTime(detectionTime);  // LE TIMER COMMENCE ICI!
            pendingSession.setStatus(SessionStatus.ACTIVE);
            activeSession = sessionRepository.save(pendingSession);
        } else {
            // Fallback: créer une nouvelle session si PENDING n'existe pas
            activeSession = ParkingSession.builder()
                    .spot(spot)
                    .driverId(reservation.getDriverId())
                    .startTime(detectionTime)
                    .status(SessionStatus.ACTIVE)
                    .build();
            activeSession = sessionRepository.save(activeSession);
        }

        // Mettre à jour le statut du spot
        spot.setStatus(false); // false = occupé
        spotRepository.save(spot);

        // Mettre à jour la réservation de PENDING → ACTIVE
        reservation.setStatus("ACTIVE");
        reservationRepository.save(reservation);

        return ParkingDetectionResponse.builder()
                .action("entry_detected")
                .spotId(spot.getId())
                .spotNumber(spot.getSpotNumber())
                .zoneName(spot.getZone().getName())
                .sessionId(activeSession.getId())
                .hasReservation(true)
                .reservation(reservation)
                .build();
    }

    private ParkingDetectionResponse handleCarExit(ParkingSpot spot, String timestamp) {
        // Trouver la session active pour ce spot
        ParkingSession activeSession = sessionRepository
                .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aucune session active trouvée pour ce spot"));

        LocalDateTime exitTime = LocalDateTime.parse(timestamp);

        // Calculer la durée
        Duration duration = Duration.between(activeSession.getStartTime(), exitTime);
        double hours = duration.toMinutes() / 60.0;
        if (hours < 0.05) hours = 0.05; // Minimum facturé

        // Calculer le coût total
        BigDecimal hourlyRate = spot.getZone().getHourlyRate();
        if (hourlyRate == null) {
            hourlyRate = BigDecimal.valueOf(10.0); // Taux par défaut
        }
        BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));

        // Mettre à jour la session
        activeSession.setEndTime(exitTime);
        activeSession.setTotalCost(totalCost);
        activeSession.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(activeSession);

        // Libérer le spot
        spot.setStatus(true); // true = libre
        spotRepository.save(spot);

        // Si une réservation était active, la terminer
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForSpotAtTime(spot.getId(), exitTime);

        if (!activeReservations.isEmpty()) {
            Reservation reservation = activeReservations.get(0);
            reservation.setStatus("COMPLETED");
            reservationRepository.save(reservation);
        }

        return ParkingDetectionResponse.builder()
                .action("exit_detected")
                .spotId(spot.getId())
                .spotNumber(spot.getSpotNumber())
                .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                .sessionId(session.getId())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .duration(String.format("%d minutes", feeCalculation.getDurationMinutes()))
                .hourlyRate(feeCalculation.getHourlyRate().doubleValue())
                .totalCost(feeCalculation.getTotalCost().doubleValue())
                .hasReservation(hadReservation)
                .spotStatus(spot.getStatus() ? "OCCUPIED" : "FREE") // ✅ CORRIGÉ
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

    // ==========================
    // ✅ INNER CLASS
    // ==========================
    private static class ParkingFeeCalculation {
        private final long durationMinutes;
        private final BigDecimal hourlyRate;
        private final BigDecimal totalCost;

        public ParkingFeeCalculation(long durationMinutes, BigDecimal hourlyRate, BigDecimal totalCost) {
            this.durationMinutes = durationMinutes;
            this.hourlyRate = hourlyRate;
            this.totalCost = totalCost;
        }

        public long getDurationMinutes() {
            return durationMinutes;
        }

        public BigDecimal getHourlyRate() {
            return hourlyRate;
        }

        public BigDecimal getTotalCost() {
            return totalCost;
        }
    }

    public void checkSpotConsistency() {
        List<ParkingSpot> allSpots = spotRepository.findAll();
        int inconsistencies = 0;

        for (ParkingSpot spot : allSpots) {
            // Vérifier si le statut du spot correspond à une session active
            boolean hasActiveSession = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE)
                    .isPresent();

            boolean shouldBeOccupied = hasActiveSession;
            boolean isOccupied = spot.getStatus() == SPOT_OCCUPIED;

            if (shouldBeOccupied != isOccupied) {
                log.warn("⚠️ Incohérence sur spot {}: Statut DB={}, Session active={}",
                        spot.getSpotNumber(), isOccupied ? "OCCUPÉ" : "LIBRE", hasActiveSession);
                inconsistencies++;

                // Corriger automatiquement
                spot.setStatus(shouldBeOccupied ? SPOT_OCCUPIED : SPOT_FREE);
                spotRepository.save(spot);
                log.info("✅ Correction appliquée pour spot {}", spot.getSpotNumber());
            }
        }

        if (inconsistencies > 0) {
            log.info("🔧 {} incohérences corrigées", inconsistencies);
        } else {
            log.info("✅ Tous les spots sont cohérents");
        }
    }
}