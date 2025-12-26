package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ParkingDetectionRequest;
import org.example.backend.DTO.ParkingDetectionResponse;
import org.example.backend.entities.ParkingSession;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.entities.Reservation;
import org.example.backend.enums.ReservationStatus;
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

    // Configuration tarifaire
    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("10.00");
    private static final BigDecimal MINIMUM_PARKING_FEE = new BigDecimal("5.00");

    // Constantes DB
    private static final boolean SPOT_FREE = true;      // true = LIBRE
    private static final boolean SPOT_OCCUPIED = false; // false = OCCUPÉ

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
                    spot.getStatus() ? "LIBRE" : "OCCUPÉ");

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

    /**
     * Timestamp parser robuste
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            if (timestamp == null || timestamp.isBlank()) {
                return LocalDateTime.now();
            }

            // ISO with Z or + offset
            if (timestamp.endsWith("Z") || timestamp.contains("+")) {
                return OffsetDateTime.parse(timestamp).toLocalDateTime();
            }

            // ISO local datetime with millis
            if (timestamp.contains("T") && timestamp.contains(".")) {
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }

            // ISO local datetime
            if (timestamp.contains("T")) {
                return LocalDateTime.parse(timestamp);
            }

            // Date only or "yyyy-MM-dd HH:mm:ss"
            String datePart = timestamp.split(" ")[0];
            LocalDateTime now = LocalDateTime.now();
            return LocalDateTime.parse(datePart + "T" +
                    String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond()));

        } catch (DateTimeParseException e) {
            log.warn("⚠️ Timestamp non reconnu: {} → utilisation de LocalDateTime.now()", timestamp);
            return LocalDateTime.now();
        } catch (Exception e) {
            log.warn("⚠️ Erreur parseTimestamp: {} → utilisation de LocalDateTime.now()", timestamp, e);
            return LocalDateTime.now();
        }
    }

    private ParkingDetectionResponse handleCarEntry(ParkingSpot spot, String timestamp) {
        try {
            log.info("🚗 ENTREE détectée pour le spot {}", spot.getSpotNumber());

            LocalDateTime detectionTime = parseTimestamp(timestamp);

            // VÉRIFICATION 1: Vérifier si le spot est déjà occupé
            if (spot.getStatus() == SPOT_OCCUPIED) {
                log.warn("⚠️ Spot {} déjà occupé - vérification des sessions", spot.getSpotNumber());

                Optional<ParkingSession> activeSessionOpt = sessionRepository
                        .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

                if (activeSessionOpt.isPresent()) {
                    ParkingSession existingSession = activeSessionOpt.get();
                    log.info("✅ Session active trouvée: {}", existingSession.getId());

                    // Vérifier s'il y a une réservation associée
                    boolean hasReservation = existingSession.getDriverId() != null &&
                            !existingSession.getDriverId().equals("anonymous");

                    return buildEntryResponse(spot, existingSession, hasReservation,
                            "Spot déjà occupé (session existante)");
                } else {
                    log.info("🔧 Correction: Spot marqué occupé mais pas de session → mise à jour");
                    spot.setStatus(SPOT_FREE);
                    spotRepository.save(spot);
                    spotRepository.flush();
                }
            }

            // VÉRIFICATION 2: Vérifier s'il y a déjà une session active
            Optional<ParkingSession> activeSessionOpt = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

            if (activeSessionOpt.isPresent()) {
                ParkingSession existingSession = activeSessionOpt.get();
                log.info("⚠️ Session déjà active: {}", existingSession.getId());
                boolean hasReservation = existingSession.getDriverId() != null &&
                        !existingSession.getDriverId().equals("anonymous");
                return buildEntryResponse(spot, existingSession, hasReservation, "Session déjà active");
            }

            // ÉTAPE CRITIQUE: Rechercher les réservations pour ce spot
            log.info("🔍 Recherche réservations pour spot ID: {}, à: {}", spot.getId(), detectionTime);

            // Vérifier d'abord s'il y a des réservations pour ce spot
            List<Reservation> allReservations = reservationRepository.findBySpotId(spot.getId());
            log.info("📋 Nombre total de réservations pour spot {}: {}", spot.getId(), allReservations.size());

            for (Reservation r : allReservations) {
                log.info("   - Réservation ID: {}, statut: {}, start: {}, end: {}, driver: {}",
                        r.getId(), r.getStatus(), r.getStartTime(), r.getEndTime(), r.getDriverId());
            }

            // Recherche spécifique: réservations actives (PENDING ou CONFIRMED) dans la plage horaire
            List<Reservation> reservations = reservationRepository
                    .findBySpotIdAndStatusInAndTimeRange(
                            spot.getId(),
                            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED),
                            detectionTime
                    );

            log.info("🔍 Réservations trouvées (PENDING/CONFIRMED): {}", reservations.size());

            boolean hasReservation = !reservations.isEmpty();
            Reservation reservation = hasReservation ? reservations.get(0) : null;

            if (hasReservation && reservation != null) {
                log.info("✅ Réservation trouvée: ID={}, driverId={}, statut actuel={}",
                        reservation.getId(), reservation.getDriverId(), reservation.getStatus());

                // VÉRIFICATION IMPORTANTE: S'assurer que la réservation n'est pas déjà active
                if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                    log.warn("⚠️ Réservation déjà ACTIVE - vérifier la cohérence");
                    // Vérifier s'il y a une session existante
                    Optional<ParkingSession> existingSession = sessionRepository
                            .findBySpotIdAndDriverIdAndStatus(spot.getId(), reservation.getDriverId(), SessionStatus.ACTIVE);

                    if (existingSession.isPresent()) {
                        return buildEntryResponse(spot, existingSession.get(), true,
                                "Réservation déjà active avec session existante");
                    }
                }

                // Mettre à jour la réservation en ACTIVE
                reservation.setStatus(ReservationStatus.ACTIVE);
                Reservation updatedReservation = reservationRepository.save(reservation);
                reservationRepository.flush();
                log.info("✅ Réservation {} passée à ACTIVE (driver: {})",
                        updatedReservation.getId(), updatedReservation.getDriverId());
            } else {
                log.info("❌ Aucune réservation PENDING/CONFIRMED trouvée pour spot {} à {}",
                        spot.getSpotNumber(), detectionTime);

                // Vérifier s'il y a une réservation ACTIVE mais non trouvée par la requête temporelle
                List<Reservation> activeReservations = reservationRepository
                        .findBySpotIdAndStatus(spot.getId(), ReservationStatus.ACTIVE);

                if (!activeReservations.isEmpty()) {
                    reservation = activeReservations.get(0);
                    hasReservation = true;
                    log.info("📌 Réservation ACTIVE existante trouvée: ID={}, driver={}",
                            reservation.getId(), reservation.getDriverId());
                }
            }

            // Créer une nouvelle session
            String driverId = hasReservation && reservation != null ?
                    reservation.getDriverId() : "anonymous";

            ParkingSession newSession = ParkingSession.builder()
                    .spot(spot)
                    .driverId(driverId)
                    .startTime(detectionTime)
                    .status(SessionStatus.ACTIVE)
                    .totalCost(BigDecimal.ZERO)
                    .build();

            log.info("🟦 Création session: spotId={}, driverId={}, startTime={}, hasReservation={}",
                    spot.getId(), driverId, detectionTime, hasReservation);

            ParkingSession savedSession = sessionRepository.save(newSession);
            sessionRepository.flush();

            log.info("✅ Session créée: ID={}", savedSession.getId());

            // Mettre le spot en OCCUPÉ
            spot.setStatus(SPOT_OCCUPIED);
            spotRepository.save(spot);
            spotRepository.flush();

            log.info("✅ Spot {} maintenant OCCUPÉ - Session ID: {}",
                    spot.getSpotNumber(), savedSession.getId());

            return buildEntryResponse(spot, savedSession, hasReservation,
                    hasReservation ? "Entrée avec réservation activée" : "Entrée sans réservation");

        } catch (Exception e) {
            log.error("❌ Erreur dans handleCarEntry: ", e);
            e.printStackTrace();
            return buildErrorResponse(spot, "Erreur lors de l'entrée: " + e.getMessage());
        }
    }

    private ParkingDetectionResponse handleCarExit(ParkingSpot spot, String timestamp) {
        try {
            log.info("🚪 SORTIE détectée pour le spot {}", spot.getSpotNumber());

            Optional<ParkingSession> activeSessionOpt = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

            if (activeSessionOpt.isEmpty()) {
                if (spot.getStatus() == SPOT_OCCUPIED) {
                    log.warn("⚠️ Spot {} occupé sans session active - correction",
                            spot.getSpotNumber());
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

            ParkingSession activeSession = activeSessionOpt.get();
            LocalDateTime exitTime = parseTimestamp(timestamp);

            if (exitTime.isBefore(activeSession.getStartTime())) {
                log.error("❌ Sortie {} < Entrée {}", exitTime, activeSession.getStartTime());
                exitTime = LocalDateTime.now();
            }

            Duration duration = Duration.between(activeSession.getStartTime(), exitTime);
            long minutes = Math.max(1, duration.toMinutes());
            double hours = Math.ceil(minutes / 60.0);

            BigDecimal hourlyRate = getHourlyRate(spot);
            BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));

            if (totalCost.compareTo(MINIMUM_PARKING_FEE) < 0) {
                totalCost = MINIMUM_PARKING_FEE;
            }
            totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

            activeSession.setEndTime(exitTime);
            activeSession.setTotalCost(totalCost);
            activeSession.setStatus(SessionStatus.COMPLETED);
            sessionRepository.save(activeSession);
            sessionRepository.flush();

            // Mettre à jour la réservation si elle existe
            if (activeSession.getDriverId() != null && !activeSession.getDriverId().equals("anonymous")) {
                updateReservationAfterExit(activeSession.getDriverId(), spot.getId(), exitTime);
            }

            spot.setStatus(SPOT_FREE);
            spotRepository.save(spot);
            spotRepository.flush();

            boolean hadReservation = activeSession.getDriverId() != null &&
                    !activeSession.getDriverId().equals("anonymous");

            return buildExitResponse(spot, activeSession, minutes, hourlyRate, totalCost,
                    hadReservation, "Sortie enregistrée avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur dans handleCarExit: ", e);
            return buildErrorResponse(spot, "Erreur lors de la sortie: " + e.getMessage());
        }
    }

    /**
     * Met à jour le statut de la réservation après la sortie
     */
    private void updateReservationAfterExit(String driverId, Long spotId, LocalDateTime exitTime) {
        if (driverId == null || driverId.equals("anonymous")) {
            return;
        }

        try {
            // Trouver la réservation ACTIVE pour ce driver et ce spot
            List<Reservation> activeReservations = reservationRepository
                    .findByDriverIdAndSpotIdAndStatus(driverId, spotId, ReservationStatus.ACTIVE);

            if (!activeReservations.isEmpty()) {
                Reservation reservation = activeReservations.get(0);
                reservation.setStatus(ReservationStatus.COMPLETED);
                // Optionnel: ajuster l'heure de fin
                if (reservation.getEndTime().isBefore(exitTime)) {
                    reservation.setEndTime(exitTime);
                }
                reservationRepository.save(reservation);
                reservationRepository.flush();
                log.info("✅ Réservation {} passée à COMPLETED après sortie (driver: {})",
                        reservation.getId(), driverId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur lors de la mise à jour de la réservation après sortie: {}", e.getMessage());
        }
    }

    private BigDecimal getHourlyRate(ParkingSpot spot) {
        if (spot.getHourlyRate() != null && spot.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return spot.getHourlyRate();
        }

        if (spot.getZone() != null) {
            try {
                var zoneRate = zoneRateService.getZoneRateById(spot.getZone().getId());
                if (zoneRate != null && zoneRate.getCurrentRate() != null) {
                    return zoneRate.getCurrentRate();
                }
            } catch (Exception e) {
                log.warn("Erreur lors de la récupération du taux de zone: {}", e.getMessage());
            }
        }

        return DEFAULT_HOURLY_RATE;
    }

    // ==========================
    // RESPONSE BUILDERS
    // ==========================
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
                .driverId(session.getDriverId())
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
                .duration(String.format("%d minutes", durationMinutes))
                .hourlyRate(hourlyRate.doubleValue())
                .totalCost(totalCost.doubleValue())
                .hasReservation(hadReservation)
                .driverId(session.getDriverId())
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

    @Transactional
    public String resetAllSpots() {
        try {
            List<ParkingSpot> allSpots = spotRepository.findAll();
            for (ParkingSpot spot : allSpots) {
                spot.setStatus(SPOT_FREE);
            }
            spotRepository.saveAll(allSpots);
            spotRepository.flush();

            List<ParkingSession> activeSessions = sessionRepository.findByStatus(SessionStatus.ACTIVE);
            for (ParkingSession session : activeSessions) {
                session.setStatus(SessionStatus.COMPLETED);
                session.setEndTime(LocalDateTime.now());
            }
            sessionRepository.saveAll(activeSessions);
            sessionRepository.flush();

            return String.format("✅ Réinitialisation: %d spots libérés, %d sessions terminées",
                    allSpots.size(), activeSessions.size());

        } catch (Exception e) {
            log.error("❌ Erreur resetAllSpots: ", e);
            return "Erreur: " + e.getMessage();
        }
    }
}