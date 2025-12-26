package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.ReservationRequestDTO;
import org.example.backend.DTO.ReservationResponseDTO;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.entities.Reservation;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpotRepository spotRepository;

    public ReservationResponseDTO createReservation(ReservationRequestDTO request) {
        // Vérifier que le spot existe
        ParkingSpot spot = spotRepository.findById(request.getSpotId())
                .orElseThrow(() -> new RuntimeException("Spot non trouvé avec l'ID: " + request.getSpotId()));

        // Vérifier que le spot est disponible pour la période demandée
        if (isSpotReserved(request.getSpotId(), request.getStartTime(), request.getEndTime())) {
            throw new RuntimeException("Le spot n'est pas disponible pour la période demandée");
        }

        // Vérifier que le spot est actuellement libre
        if (Boolean.FALSE.equals(spot.getStatus())) {
            throw new RuntimeException("Le spot est actuellement occupé");
        }

        // Créer la réservation
        Reservation reservation = Reservation.builder()
                .spot(spot)
                .driverId(request.getDriverId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status("CONFIRMED")
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

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
        if ("COMPLETED".equals(reservation.getStatus()) || "CANCELLED".equals(reservation.getStatus())) {
            throw new RuntimeException("La réservation ne peut pas être annulée");
        }

        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);
    }

    private boolean isSpotReserved(Long spotId, LocalDateTime startTime, LocalDateTime endTime) {
        return reservationRepository.isSpotReservedForPeriod(spotId, startTime, endTime);
    }
}