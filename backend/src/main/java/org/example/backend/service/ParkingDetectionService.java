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

            boolean hasReservation = !activeReservations.isEmpty();
            Reservation reservation = hasReservation ? activeReservations.get(0) : null;

            ParkingSession newSession = ParkingSession.builder()
                    .spot(spot)
                    .driverId(hasReservation && reservation != null ? reservation.getDriverId() : "anonymous")
                    .startTime(detectionTime)
                    .status(SessionStatus.ACTIVE)
                    .totalCost(BigDecimal.ZERO)
                    .build();

            sessionRepository.save(newSession);

            // Marquer le spot comme OCCUPÉ (true = 1)
            spot.setStatus(SPOT_OCCUPIED);
            spotRepository.save(spot);

            if (hasReservation && reservation != null) {
                reservation.setStatus("ACTIVE");
                reservationRepository.save(reservation);
                log.info("Réservation {} activée", reservation.getId());
            }

            log.info("✅ Nouvelle session créée: ID={}, Spot maintenant OCCUPÉ", newSession.getId());
            return buildEntryResponse(spot, newSession, hasReservation, "Entrée enregistrée avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur dans handleCarEntry: ", e);
            return buildErrorResponse(spot, "Erreur lors de l'entrée: " + e.getMessage());
        }
    }

    private ParkingDetectionResponse handleCarExit(ParkingSpot spot, String timestamp) {
        try {
            log.info("🚪 SORTIE détectée pour le spot {}", spot.getSpotNumber());

            Optional<ParkingSession> activeSessionOpt = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE);

            // Gestion des cas où il n'y a pas de session active
            if (activeSessionOpt.isEmpty()) {
                // Vérifier si le spot est marqué comme occupé
                if (spot.getStatus() == SPOT_OCCUPIED) {
                    log.warn("⚠️ Spot {} marqué occupé mais pas de session active - correction du statut",
                            spot.getSpotNumber());

                    // Corriger le statut du spot
                    spot.setStatus(SPOT_FREE);
                    spotRepository.save(spot);

                    return ParkingDetectionResponse.builder()
                            .action("exit_corrected")
                            .spotId(spot.getId())
                            .spotNumber(spot.getSpotNumber())
                            .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                            .message("⚠️ Statut corrigé: Spot marqué libre (pas de session active)")
                            .build();
                }

                log.warn("⚠️ Aucune session active pour le spot {} déjà libre - Sortie ignorée", spot.getSpotNumber());
                return ParkingDetectionResponse.builder()
                        .action("exit_ignored")
                        .spotId(spot.getId())
                        .spotNumber(spot.getSpotNumber())
                        .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                        .message("⚠️ Aucune session active - Sortie ignorée")
                        .build();
            }

            ParkingSession activeSession = activeSessionOpt.get();
            LocalDateTime exitTime = parseTimestamp(timestamp);

            // Protection contre les timestamps incohérents
            if (exitTime.isBefore(activeSession.getStartTime())) {
                log.error("❌ Heure de sortie {} antérieure à l'entrée {}",
                        exitTime, activeSession.getStartTime());
                exitTime = LocalDateTime.now();
            }

            ParkingFeeCalculation feeCalculation = calculateParkingFee(activeSession, exitTime, spot);

            // Mise à jour session
            activeSession.setEndTime(exitTime);
            activeSession.setTotalCost(feeCalculation.getTotalCost());
            activeSession.setStatus(SessionStatus.COMPLETED);
            sessionRepository.save(activeSession);

            // IMPORTANT: Marquer le spot comme LIBRE (false = 0)
            spot.setStatus(SPOT_FREE);
            spotRepository.save(spot);

            boolean hadReservation = activeSession.getDriverId() != null &&
                    !activeSession.getDriverId().equals("anonymous");

            log.info("✅ Session {} terminée - Coût: {}€, Durée: {} minutes, Spot maintenant LIBRE",
                    activeSession.getId(), feeCalculation.getTotalCost(), feeCalculation.getDurationMinutes());

            return buildExitResponse(spot, activeSession, feeCalculation, hadReservation,
                    "✅ Sortie enregistrée avec succès - Spot libéré");

        } catch (Exception e) {
            log.error("❌ Erreur dans handleCarExit: ", e);
            return buildErrorResponse(spot, "Erreur lors de la sortie: " + e.getMessage());
        }
    }

    private ParkingFeeCalculation calculateParkingFee(ParkingSession session,
                                                      LocalDateTime exitTime,
                                                      ParkingSpot spot) {
        LocalDateTime startTime = session.getStartTime();
        Duration duration = Duration.between(startTime, exitTime);
        long minutes = Math.max(1, duration.toMinutes());

        BigDecimal hourlyRate = calculateDynamicRate(spot, startTime);

        // Facturation par heure entamée (arrondi supérieur)
        double hours = Math.ceil(minutes / 60.0);
        BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));

        // Application du minimum obligatoire
        if (totalCost.compareTo(MINIMUM_PARKING_FEE) < 0) {
            totalCost = MINIMUM_PARKING_FEE;
        }

        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

        log.info("💰 Calcul tarifaire - Durée: {}min ({}h), Taux: {}€/h → Total: {}€",
                minutes, String.format("%.2f", hours), hourlyRate, totalCost);

        return new ParkingFeeCalculation(minutes, hourlyRate, totalCost);
    }

    private BigDecimal calculateDynamicRate(ParkingSpot spot, LocalDateTime timestamp) {
        BigDecimal baseRate = getBaseRate(spot);
        BigDecimal adjustedRate = baseRate;

        if (isNightTime(timestamp)) {
            adjustedRate = adjustedRate.multiply(NIGHT_RATE_MULTIPLIER);
            log.debug("Ajustement nuit (-30%): {} -> {}", baseRate, adjustedRate);
        }

        if (isWeekend(timestamp)) {
            adjustedRate = adjustedRate.multiply(WEEKEND_RATE_MULTIPLIER);
            log.debug("Ajustement weekend (+20%): {} -> {}", baseRate, adjustedRate);
        }

        return adjustedRate.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getBaseRate(ParkingSpot spot) {
        // Priorité 1: Taux du spot
        if (spot.getHourlyRate() != null && spot.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Utilisation du taux du spot: {}", spot.getHourlyRate());
            return spot.getHourlyRate();
        }

        // Priorité 2: Taux de la zone
        if (spot.getZone() != null) {
            try {
                var zoneRate = zoneRateService.getZoneRateById(spot.getZone().getId());
                if (zoneRate != null && zoneRate.getCurrentRate() != null) {
                    log.debug("Utilisation du taux de la zone {}: {}",
                            spot.getZone().getName(), zoneRate.getCurrentRate());
                    return zoneRate.getCurrentRate();
                }
            } catch (Exception e) {
                log.warn("Erreur lors de la récupération du taux de zone: {}", e.getMessage());
            }
        }

        // Priorité 3: Taux par défaut
        log.debug("Utilisation du taux par défaut: {}", DEFAULT_HOURLY_RATE);
        return DEFAULT_HOURLY_RATE;
    }

    private boolean isNightTime(LocalDateTime time) {
        int hour = time.getHour();
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR;
    }

    private boolean isWeekend(LocalDateTime time) {
        return time.getDayOfWeek().getValue() >= 6;
    }

    // ==========================
    // ✅ RESPONSE BUILDERS
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
                .spotStatus(spot.getStatus() ? "OCCUPIED" : "FREE") // ✅ CORRIGÉ
                .message(message)
                .build();
    }

    private ParkingDetectionResponse buildExitResponse(ParkingSpot spot, ParkingSession session,
                                                       ParkingFeeCalculation feeCalculation,
                                                       boolean hadReservation, String message) {
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