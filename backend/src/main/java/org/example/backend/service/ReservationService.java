package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.ReservationRequestDTO;
import org.example.backend.DTO.ReservationResponseDTO;
import org.example.backend.entities.ParkingSession;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.entities.Reservation;
import org.example.backend.enums.SessionStatus;
import org.example.backend.repository.ParkingSessionRepository;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;

    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO request) {
        // Vérifier que le spot existe
        ParkingSpot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new RuntimeException("Spot non trouvé avec l'ID: " + request.getSpotId()));

        // Vérifier que le spot est actuellement libre (true = free, false = occupied)
        if (Boolean.FALSE.equals(spot.getStatus())) {
            throw new RuntimeException("Le spot est actuellement occupé");
        }

        // Marquer le spot comme réservé (0/false)
        spot.setStatus(false);
        spotRepository.save(spot);

        // Empêcher plusieurs sessions PENDING pour un même spot
        // (Le capteur va ensuite transformer PENDING -> ACTIVE)
        sessionRepository.findBySpotIdAndStatus(spot.getId(), SessionStatus.PENDING)
                .ifPresent(existing -> {
                    throw new RuntimeException("Une session PENDING existe déjà pour ce spot");
                });

        // Créer la réservation avec statut PENDING
        // Le statut passera à ACTIVE quand le capteur détectera l'entrée du véhicule
        Reservation reservation = Reservation.builder()
                .spot(spot)
                .driverId(request.getDriverId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status("PENDING")
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        System.out.println("✅ Reservation saved with ID: " + savedReservation.getId() + ", status: " + savedReservation.getStatus());

        // Créer la session PENDING immédiatement (visible dans parking_sessions)
        // startTime doit rester null: le timer démarre à la détection (sensor occupied)
        ParkingSession pendingSession = ParkingSession.builder()
                .spot(spot)
                .driverId(savedReservation.getDriverId())
                .status(SessionStatus.PENDING)
                .build();
        ParkingSession savedSession = sessionRepository.save(pendingSession);
        System.out.println("✅ ParkingSession (PENDING) saved with ID: " + savedSession.getId());

        return ReservationResponseDTO.builder()
                .id(savedReservation.getId())
                .spotId(savedReservation.getSpot().getId())
                .spotNumber(savedReservation.getSpot().getSpotNumber())
                .driverId(savedReservation.getDriverId())
                .startTime(savedReservation.getStartTime())
                .endTime(savedReservation.getEndTime())
                .status(savedReservation.getStatus())
                .build();
    }

    public ReservationResponseDTO getReservationStatus(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée avec l'ID: " + id));

        return ReservationResponseDTO.builder()
                .id(reservation.getId())
                .spotId(reservation.getSpot().getId())
                .spotNumber(reservation.getSpot().getSpotNumber())
                .driverId(reservation.getDriverId())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .build();
    }

    public List<ReservationResponseDTO> getUserReservations(String driverId) {
        List<Reservation> reservations = reservationRepository.findByDriverId(driverId);

        return reservations.stream()
                .map(reservation -> ReservationResponseDTO.builder()
                        .id(reservation.getId())
                        .spotId(reservation.getSpot().getId())
                        .spotNumber(reservation.getSpot().getSpotNumber())
                        .driverId(reservation.getDriverId())
                        .startTime(reservation.getStartTime())
                        .endTime(reservation.getEndTime())
                        .status(reservation.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    public void cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Vérifier que la réservation peut être annulée
        if ("COMPLETED".equals(reservation.getStatus()) || "CANCELLED".equals(reservation.getStatus()) || "ACTIVE".equals(reservation.getStatus())) {
            throw new RuntimeException("La réservation ne peut pas être annulée");
        }

        // Annuler la réservation
        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);

        // Annuler aussi la session si elle est encore PENDING
        sessionRepository.findBySpotIdAndStatus(reservation.getSpot().getId(), SessionStatus.PENDING)
                .ifPresent(session -> {
                    if (reservation.getDriverId() != null && reservation.getDriverId().equals(session.getDriverId())) {
                        session.setStatus(SessionStatus.CANCELLED);
                        sessionRepository.save(session);
                    }
                });

        // Libérer le spot (1/true)
        ParkingSpot spot = reservation.getSpot();
        spot.setStatus(true);
        spotRepository.save(spot);
    }
}