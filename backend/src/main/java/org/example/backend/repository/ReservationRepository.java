package org.example.backend.repository;

import org.example.backend.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Trouver les réservations actives pour un spot
    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId AND r.status IN ('CONFIRMED', 'ACTIVE') " +
            "AND ((r.startTime <= :currentTime AND r.endTime >= :currentTime) OR " +
            "(r.startTime >= :startTime AND r.startTime <= :endTime))")
    List<Reservation> findActiveReservationsForSpot(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Trouver les réservations actives pour un spot à un moment donné
    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId AND r.status = 'CONFIRMED' " +
            "AND :currentTime BETWEEN r.startTime AND r.endTime")
    List<Reservation> findConfirmedReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime);

    // Trouver les réservations par driverId
    List<Reservation> findByDriverId(String driverId);

    // Trouver une réservation par son ID
    Optional<Reservation> findById(Long id);

    // Vérifier la disponibilité d'un spot pour une période
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.spot.id = :spotId " +
            "AND r.status IN ('CONFIRMED', 'ACTIVE') " +
            "AND ((r.startTime <= :endTime AND r.endTime >= :startTime))")
    boolean isSpotReservedForPeriod(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}