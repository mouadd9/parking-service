package org.example.backend.repository;

import org.example.backend.entities.ParkingSession;
import org.example.backend.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    // Pour l'historique (Déjà fait ou à garder)
    @Query("SELECT ps FROM ParkingSession ps WHERE ps.spot.id = :spotId AND ps.status = :status")
    Optional<ParkingSession> findBySpotIdAndStatus(
            @Param("spotId") Long spotId,
            @Param("status") SessionStatus status);

    // Compter par statut (méthode générée par Spring Data JPA)
    long countByStatus(String status);

    // Revenu total
    @Query("SELECT SUM(p.totalCost) FROM ParkingSession p WHERE p.totalCost IS NOT NULL")
    BigDecimal calculateTotalRevenue();

    // Revenu d'aujourd'hui
    @Query("SELECT SUM(ps.totalCost) FROM ParkingSession ps WHERE DATE(ps.endTime) = CURRENT_DATE AND ps.totalCost IS NOT NULL")
    BigDecimal calculateTodayRevenue();

    // Nouvelle méthode pour calculer le revenu par zone sur une période
    @Query("SELECT SUM(ps.totalCost) FROM ParkingSession ps " +
            "JOIN ps.spot s " +
            "JOIN s.zone z " +
            "WHERE z.id = :zoneId " +
            "AND ps.status = 'COMPLETED' " +
            "AND ps.endTime BETWEEN :startDate AND :endDate " +
            "AND ps.totalCost IS NOT NULL")
    BigDecimal calculateRevenueForZone(
            @Param("zoneId") Long zoneId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Méthode pour trouver toutes les sessions actives
    @Query("SELECT ps FROM ParkingSession ps WHERE ps.status = 'ACTIVE'")
    List<ParkingSession> findAllActiveSessions();

    // Méthode pour vérifier si un spot a une session active
    @Query("SELECT COUNT(ps) > 0 FROM ParkingSession ps WHERE ps.spot.id = :spotId AND ps.status = 'ACTIVE'")
    boolean existsActiveSessionBySpotId(@Param("spotId") Long spotId);

    // Méthodes pour gérer les sessions par driverId
    List<ParkingSession> findByDriverId(String driverId);

    // Pour vérifier si l'utilisateur a DÉJÀ une session (Bloquer Check-in)
    // et pour afficher la session active en haut de la map
    Optional<ParkingSession> findByDriverIdAndStatus(String driverId, SessionStatus status);

    List<ParkingSession> findAllByDriverIdAndStatus(String driverId, SessionStatus status);
}