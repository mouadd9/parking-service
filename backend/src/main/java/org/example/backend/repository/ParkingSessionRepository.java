package org.example.backend.repository;

import org.example.backend.entities.ParkingSession;
import org.example.backend.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    // Pour l'historique (Déjà fait ou à garder)

    // this will be used to get sessions by driverID
    List<ParkingSession> findByDriverId(String driverId);

    // 1. Pour vérifier si l'utilisateur a DÉJÀ une session (Bloquer Check-in)

    // 2. Pour afficher la session active en haut de la map
    // this returns the current session for a driver
    Optional<ParkingSession> findByDriverIdAndStatus(String driverId, SessionStatus status);
    List<ParkingSession> findAllByDriverIdAndStatus(String driverId, SessionStatus status);
}
