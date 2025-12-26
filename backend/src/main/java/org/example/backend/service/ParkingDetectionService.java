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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingDetectionService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ParkingDetectionResponse handleDetection(ParkingDetectionRequest request) {
        log.info("=== DÉTECTION REÇUE ===");
        log.info("SensorId: {}, Status: {}, Timestamp: {}",
                request.getSensorId(), request.getStatus(), request.getTimestamp());

        try {
            // Vérifier si le capteur existe
            List<ParkingSpot> allSpots = spotRepository.findAll();
            log.info("Total spots dans la base: {}", allSpots.size());

            // Log tous les capteurs
            allSpots.forEach(spot -> {
                if (spot.getSensorId() != null) {
                    log.debug("Capteur: {} -> Spot {}", spot.getSensorId(), spot.getSpotNumber());
                }
            });

            ParkingSpot spot = spotRepository.findBySensorId(request.getSensorId())
                    .orElse(null);

            if (spot == null) {
                log.error("❌ Capteur non trouvé: {}", request.getSensorId());
                return ParkingDetectionResponse.builder()
                        .action("error")
                        .message("Capteur non trouvé: " + request.getSensorId())
                        .build();
            }

            log.info("✅ Spot trouvé: ID={}, Numéro={}, Zone={}",
                    spot.getId(), spot.getSpotNumber(),
                    spot.getZone() != null ? spot.getZone().getName() : "N/A");

            // Traiter selon le status
            if ("occupied".equalsIgnoreCase(request.getStatus())) {
                return handleCarEntry(spot, request.getTimestamp());
            } else if ("free".equalsIgnoreCase(request.getStatus())) {
                return handleCarExit(spot, request.getTimestamp());
            } else {
                log.error("Statut invalide: {}", request.getStatus());
                return ParkingDetectionResponse.builder()
                        .action("error")
                        .message("Statut invalide: " + request.getStatus())
                        .build();
            }

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
            // Essayer différents formats
            if (timestamp.contains("T") && timestamp.contains(".")) {
                return LocalDateTime.parse(timestamp,
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } else if (timestamp.contains("T")) {
                return LocalDateTime.parse(timestamp);
            } else {
                // Format simple
                return LocalDateTime.parse(timestamp + "T00:00:00");
            }
        } catch (DateTimeParseException e) {
            log.warn("Format de timestamp non reconnu: {}, utilisation de l'heure actuelle", timestamp);
            return LocalDateTime.now();
        }
    }

    private ParkingDetectionResponse handleCarEntry(ParkingSpot spot, String timestamp) {
        try {
            log.info("🚗 ENTREE détectée pour le spot {}", spot.getSpotNumber());

            // Vérifier s'il y a déjà une session active
            ParkingSession existingSession = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE)
                    .orElse(null);

            if (existingSession != null) {
                log.info("Spot déjà occupé, session existante: {}", existingSession.getId());
                return ParkingDetectionResponse.builder()
                        .action("entry_detected")
                        .spotId(spot.getId())
                        .spotNumber(spot.getSpotNumber())
                        .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                        .sessionId(existingSession.getId())
                        .hasReservation(false)
                        .message("Spot déjà occupé")
                        .build();
            }

            // Parser le timestamp
            LocalDateTime detectionTime = parseTimestamp(timestamp);

            // Vérifier les réservations
            List<Reservation> activeReservations = reservationRepository
                    .findConfirmedReservationsForSpotAtTime(spot.getId(), detectionTime);

            boolean hasReservation = !activeReservations.isEmpty();
            Reservation reservation = hasReservation ? activeReservations.get(0) : null;

            // Créer nouvelle session
            ParkingSession newSession = ParkingSession.builder()
                    .spot(spot)
                    .driverId(hasReservation && reservation != null ? reservation.getDriverId() : "anonymous")
                    .startTime(detectionTime)
                    .status(SessionStatus.ACTIVE)
                    .build();

            sessionRepository.save(newSession);
            log.info("✅ Nouvelle session créée: ID={}", newSession.getId());

            // Mettre à jour le spot
            spot.setStatus(false); // Occupé
            spotRepository.save(spot);

            // Mettre à jour la réservation si elle existe
            if (hasReservation && reservation != null) {
                reservation.setStatus("ACTIVE");
                reservationRepository.save(reservation);
                log.info("Réservation {} activée", reservation.getId());
            }

            return ParkingDetectionResponse.builder()
                    .action("entry_detected")
                    .spotId(spot.getId())
                    .spotNumber(spot.getSpotNumber())
                    .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                    .sessionId(newSession.getId())
                    .hasReservation(hasReservation)
                    .reservation(hasReservation ? reservation : null)
                    .message("Entrée enregistrée avec succès")
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur dans handleCarEntry: ", e);
            return ParkingDetectionResponse.builder()
                    .action("error")
                    .spotId(spot.getId())
                    .spotNumber(spot.getSpotNumber())
                    .message("Erreur lors de l'entrée: " + e.getMessage())
                    .build();
        }
    }

    private ParkingDetectionResponse handleCarExit(ParkingSpot spot, String timestamp) {
        try {
            log.info("🚪 SORTIE détectée pour le spot {}", spot.getSpotNumber());

            // Trouver session active
            ParkingSession activeSession = sessionRepository
                    .findBySpotIdAndStatus(spot.getId(), SessionStatus.ACTIVE)
                    .orElse(null);

            if (activeSession == null) {
                log.warn("Aucune session active pour le spot {}, création d'une session factice", spot.getSpotNumber());

                // Créer une session factice pour éviter l'erreur
                ParkingSession fakeSession = ParkingSession.builder()
                        .spot(spot)
                        .driverId("unknown")
                        .startTime(LocalDateTime.now().minusHours(1))
                        .status(SessionStatus.ACTIVE)
                        .build();

                activeSession = fakeSession;
            }

            LocalDateTime exitTime = parseTimestamp(timestamp);

            // Calculer durée
            Duration duration = Duration.between(activeSession.getStartTime(), exitTime);
            double hours = Math.max(duration.toMinutes() / 60.0, 0.05);

            // Calculer coût
            BigDecimal hourlyRate = BigDecimal.valueOf(5.0); // Taux par défaut
            if (spot.getZone() != null && spot.getZone().getHourlyRate() != null) {
                hourlyRate = spot.getZone().getHourlyRate();
            }

            BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));

            // Mettre à jour session
            if (activeSession.getId() != null) {
                activeSession.setEndTime(exitTime);
                activeSession.setTotalCost(totalCost);
                activeSession.setStatus(SessionStatus.COMPLETED);
                sessionRepository.save(activeSession);
                log.info("✅ Session {} terminée", activeSession.getId());
            }

            // Libérer le spot
            spot.setStatus(true); // Libre
            spotRepository.save(spot);

            // Vérifier si c'était une réservation
            boolean hadReservation = false;
            if (activeSession.getDriverId() != null && !activeSession.getDriverId().equals("anonymous")) {
                hadReservation = true;
            }

            return ParkingDetectionResponse.builder()
                    .action("exit_detected")
                    .spotId(spot.getId())
                    .spotNumber(spot.getSpotNumber())
                    .zoneName(spot.getZone() != null ? spot.getZone().getName() : "N/A")
                    .sessionId(activeSession.getId())
                    .duration(String.format("%.2f heures", hours))
                    .totalCost(totalCost.doubleValue())
                    .hasReservation(hadReservation)
                    .message("Sortie enregistrée avec succès")
                    .build();

        } catch (Exception e) {
            log.error("❌ Erreur dans handleCarExit: ", e);
            return ParkingDetectionResponse.builder()
                    .action("error")
                    .spotId(spot.getId())
                    .spotNumber(spot.getSpotNumber())
                    .message("Erreur lors de la sortie: " + e.getMessage())
                    .build();
        }
    }
}