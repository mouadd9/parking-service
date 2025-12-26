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



    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId AND r.status IN ('PENDING', 'CONFIRMED') AND :time BETWEEN r.startTime AND r.endTime")
    List<Reservation> findPendingOrConfirmedReservationsForSpotAtTime(@Param("spotId") Long spotId, @Param("time") LocalDateTime time);

    @Query("SELECT r FROM Reservation r WHERE r.driverId = :driverId AND r.spot.id = :spotId AND r.status = 'ACTIVE'")
    Optional<Reservation> findActiveReservationForDriverAndSpot(@Param("driverId") String driverId, @Param("spotId") Long spotId);
    // Trouver les réservations actives ou en attente pour un spot
    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId AND r.status IN ('PENDING', 'ACTIVE') " +
            "AND ((r.startTime <= :currentTime AND r.endTime >= :currentTime) OR " +
            "(r.startTime >= :startTime AND r.startTime <= :endTime))")
    List<Reservation> findActiveReservationsForSpot(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
           SELECT r 
           FROM Reservation r
           WHERE r.spot.id = :spotId
             AND r.status = 'CONFIRMED'
             AND :time BETWEEN r.startTime AND r.endTime
           ORDER BY r.startTime DESC
           """)
    List<Reservation> findConfirmedReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("time") LocalDateTime time
    );

    // Trouver les réservations PENDING pour un spot à un moment donné (attente d'entrée physique)
    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId AND r.status = 'PENDING' " +
            "AND :currentTime BETWEEN r.startTime AND r.endTime")
    List<Reservation> findPendingReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime);

    // Trouver les réservations ACTIVE pour un spot (utilisateur déjà entré)
    @Query("SELECT r FROM Reservation r WHERE r.spot.id = :spotId AND r.status = 'ACTIVE' " +
            "AND :currentTime BETWEEN r.startTime AND r.endTime")
    List<Reservation> findActiveReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime);

    // Trouver les réservations par driverId
    List<Reservation> findByDriverId(String driverId);

    // Trouver une réservation par son ID
    Optional<Reservation> findById(Long id);

    // Vérifier la disponibilité d'un spot pour une période
    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.spot.id = :spotId " +
            "AND r.status IN ('PENDING', 'ACTIVE') " +
            "AND ((r.startTime <= :endTime AND r.endTime >= :startTime))")
    boolean isSpotReservedForPeriod(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}