package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingDetectionService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ParkingDetectionResponse handleDetection(ParkingDetectionRequest request) {
        // Trouver le spot par sensorId
        ParkingSpot spot = spotRepository.findBySensorId(request.getSensorId())
                .orElseThrow(() -> new RuntimeException("Capteur non trouvé: " + request.getSensorId()));

        // Si le capteur indique "occupied"
        if ("occupied".equals(request.getStatus())) {
            return handleCarEntry(spot, request.getTimestamp());
        }
        // Si le capteur indique "free"
        else if ("free".equals(request.getStatus())) {
            return handleCarExit(spot, request.getTimestamp());
        } else {
            throw new RuntimeException("Statut de capteur invalide: " + request.getStatus());
        }
    }

    private ParkingDetectionResponse handleCarEntry(ParkingSpot spot, String timestamp) {
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
                .zoneName(spot.getZone().getName())
                .sessionId(activeSession.getId())
                .hasReservation(false)
                .duration(String.format("%.2f heures", hours))
                .totalCost(totalCost.doubleValue())
                .build();
    }
}