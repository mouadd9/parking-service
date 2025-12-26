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

    List<Reservation> findBySpotId(Long spotId);

    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status IN :statuses
          AND :time BETWEEN r.startTime AND r.endTime
        ORDER BY r.startTime DESC
    """)
    List<Reservation> findBySpotIdAndStatusInAndTimeRange(
            @Param("spotId") Long spotId,
            @Param("statuses") List<String> statuses,
            @Param("time") LocalDateTime time
    );

    List<Reservation> findBySpotIdAndStatus(Long spotId, String status);

    @Query("""
        SELECT r
        FROM Reservation r
        WHERE r.driverId = :driverId
          AND r.spot.id = :spotId
          AND r.status = :status
        ORDER BY r.startTime DESC
    """)
    List<Reservation> findByDriverIdAndSpotIdAndStatus(
            @Param("driverId") String driverId,
            @Param("spotId") Long spotId,
            @Param("status") String status
    );

    // ✅ PENDING reservations dans l'intervalle
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status = 'PENDING'
          AND :time BETWEEN r.startTime AND r.endTime
        ORDER BY r.startTime ASC
    """)
    List<Reservation> findPendingReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("time") LocalDateTime time
    );

    // ✅ CONFIRMED reservations dans l'intervalle (CORRIGÉ)
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status = 'CONFIRMED'
          AND :detectionTime BETWEEN r.startTime AND r.endTime
        ORDER BY r.startTime ASC
    """)
    List<Reservation> findConfirmedReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("detectionTime") LocalDateTime detectionTime
    );

    // ✅ Active reservation for driver and spot
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.driverId = :driverId
          AND r.spot.id = :spotId
          AND r.status = 'ACTIVE'
    """)
    Optional<Reservation> findActiveReservationForDriverAndSpot(
            @Param("driverId") String driverId,
            @Param("spotId") Long spotId
    );

    // Trouver les réservations actives ou en attente pour un spot
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status IN ('PENDING', 'ACTIVE')
          AND (
                (r.startTime <= :currentTime AND r.endTime >= :currentTime)
                OR
                (r.startTime >= :startTime AND r.startTime <= :endTime)
              )
    """)
    List<Reservation> findActiveReservationsForSpot(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Trouver les réservations ACTIVE pour un spot
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status = 'ACTIVE'
          AND :currentTime BETWEEN r.startTime AND r.endTime
    """)
    List<Reservation> findActiveReservationsForSpotAtTime(
            @Param("spotId") Long spotId,
            @Param("currentTime") LocalDateTime currentTime
    );

    List<Reservation> findByDriverId(String driverId);

    @Override
    Optional<Reservation> findById(Long id);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.spot.id = :spotId
          AND r.status IN ('PENDING', 'ACTIVE')
          AND (r.startTime <= :endTime AND r.endTime >= :startTime)
    """)
    boolean isSpotReservedForPeriod(
            @Param("spotId") Long spotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
