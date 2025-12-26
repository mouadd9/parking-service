package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entities.Claim;
import org.example.backend.enums.Role;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatisticsService {

    private final UtilisateurRepository utilisateurRepository;
    private final ParkingZoneRepository zoneRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final ClaimRepository claimRepository;

    public Map<String, Object> getStatistics() {
        log.info("Calcul des statistiques admin...");

        Map<String, Object> statistics = new HashMap<>();

        try {
            // Statistiques utilisateurs - VRAIES données
            Map<String, Object> usersStats = new HashMap<>();
            long totalUsers = utilisateurRepository.count();
            log.info("Total utilisateurs dans la base: {}", totalUsers);

            // Utilisez directement l'enum Role
            long totalDrivers = utilisateurRepository.countByRole(Role.CONDUCTEUR);
            long totalAdmins = utilisateurRepository.countByRole(Role.ADMINISTRATEUR);

            usersStats.put("total", totalUsers);
            usersStats.put("drivers", totalDrivers);
            usersStats.put("admins", totalAdmins);
            statistics.put("users", usersStats);
            log.info("Utilisateurs - Total: {}, Conducteurs: {}, Admins: {}",
                    totalUsers, totalDrivers, totalAdmins);

            // Statistiques parking - VRAIES données
            Map<String, Object> parkingStats = new HashMap<>();
            long totalZones = zoneRepository.count();
            long totalSpots = parkingSpotRepository.count();

            // Assurez-vous que la méthode countByStatus existe dans le repository
            long occupiedSpots = 0;
            long availableSpots = 0;

            try {
                occupiedSpots = parkingSpotRepository.countByStatus(false);
                availableSpots = parkingSpotRepository.countByStatus(true);
            } catch (Exception e) {
                // Si la méthode n'existe pas, calculez manuellement
                log.warn("Méthode countByStatus non disponible, calcul manuel");
                occupiedSpots = parkingSpotRepository.findAll().stream()
                        .filter(spot -> Boolean.FALSE.equals(spot.getStatus()))
                        .count();
                availableSpots = parkingSpotRepository.findAll().stream()
                        .filter(spot -> Boolean.TRUE.equals(spot.getStatus()))
                        .count();
            }

            parkingStats.put("totalZones", totalZones);
            parkingStats.put("totalSpots", totalSpots);
            parkingStats.put("occupiedSpots", occupiedSpots);
            parkingStats.put("availableSpots", availableSpots);
            statistics.put("parking", parkingStats);
            log.info("Parking - Zones: {}, Spots: {}, Occupés: {}, Libres: {}",
                    totalZones, totalSpots, occupiedSpots, availableSpots);

            // Statistiques sessions - VRAIES données
            Map<String, Object> sessionsStats = new HashMap<>();
            long totalSessions = parkingSessionRepository.count();

            long activeSessions = 0;
            long completedSessions = 0;

            try {
                activeSessions = parkingSessionRepository.countByStatus("ACTIVE");
                completedSessions = parkingSessionRepository.countByStatus("COMPLETED");
            } catch (Exception e) {
                // Calcul manuel si la méthode n'existe pas
                log.warn("Méthode countByStatus non disponible pour les sessions");
                activeSessions = parkingSessionRepository.findAll().stream()
                        .filter(session -> "ACTIVE".equals(session.getStatus()))
                        .count();
                completedSessions = parkingSessionRepository.findAll().stream()
                        .filter(session -> "COMPLETED".equals(session.getStatus()))
                        .count();
            }

            sessionsStats.put("total", totalSessions);
            sessionsStats.put("active", activeSessions);
            sessionsStats.put("completed", completedSessions);
            statistics.put("sessions", sessionsStats);
            log.info("Sessions - Total: {}, Actives: {}, Complétées: {}",
                    totalSessions, activeSessions, completedSessions);

            // Statistiques réclamations - VRAIES données
            Map<String, Object> claimsStats = new HashMap<>();
            long totalClaims = claimRepository.count();

            long pendingClaims = 0;
            long resolvedClaims = 0;

            try {
                // Essayez d'abord avec la méthode existante
                List<Claim> pendingClaimsList = claimRepository.findByCurrentStatus("PENDING");
                List<Claim> resolvedClaimsList = claimRepository.findByCurrentStatus("RESOLVED");
                pendingClaims = pendingClaimsList != null ? pendingClaimsList.size() : 0;
                resolvedClaims = resolvedClaimsList != null ? resolvedClaimsList.size() : 0;
            } catch (Exception e) {
                // Calcul manuel
                log.warn("Méthode findByCurrentStatus non disponible");
                pendingClaims = claimRepository.findAll().stream()
                        .filter(claim -> "PENDING".equals(claim.getCurrentStatus()))
                        .count();
                resolvedClaims = claimRepository.findAll().stream()
                        .filter(claim -> "RESOLVED".equals(claim.getCurrentStatus()))
                        .count();
            }

            claimsStats.put("total", totalClaims);
            claimsStats.put("pending", pendingClaims);
            claimsStats.put("resolved", resolvedClaims);
            statistics.put("reclamations", claimsStats);
            log.info("Réclamations - Total: {}, En attente: {}, Résolues: {}",
                    totalClaims, pendingClaims, resolvedClaims);

            // Statistiques financières - VRAIES données
            Map<String, Object> financialStats = new HashMap<>();
            financialStats.put("totalRevenue", calculateTotalRevenue());
            financialStats.put("todayRevenue", calculateTodayRevenue());
            statistics.put("financial", financialStats);

            log.info("✅ Statistiques VRAIES calculées: {} zones, {} spots, {} utilisateurs, {} réclamations",
                    totalZones, totalSpots, totalUsers, totalClaims);

        } catch (Exception e) {
            log.error("❌ ERREUR lors du calcul des statistiques: ", e);
            // Ne retournez PAS les données mockées
            // Lancez l'exception pour que le contrôleur la gère
            throw new RuntimeException("Erreur lors du calcul des statistiques: " + e.getMessage(), e);
        }

        return statistics;
    }

    private Double calculateTotalRevenue() {
        try {
            Object result = null;
            try {
                // Essayez d'abord la méthode du repository
                result = parkingSessionRepository.calculateTotalRevenue();
            } catch (Exception e) {
                log.warn("Méthode calculateTotalRevenue non disponible, calcul manuel");
                // Calcul manuel
                result = parkingSessionRepository.findAll().stream()
                        .filter(session -> session.getTotalCost() != null)
                        .mapToDouble(session -> session.getTotalCost().doubleValue())
                        .sum();
            }

            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            log.warn("Erreur lors du calcul du revenu total, utilisation de 0.0");
            return 0.0;
        }
    }

    private Double calculateTodayRevenue() {
        try {
            Object result = null;
            try {
                result = parkingSessionRepository.calculateTodayRevenue();
            } catch (Exception e) {
                log.warn("Méthode calculateTodayRevenue non disponible, calcul manuel");
                // Calcul manuel simplifié (pour le développement)
                result = parkingSessionRepository.findAll().stream()
                        .filter(session -> session.getTotalCost() != null
                                && session.getEndTime() != null
                                && session.getEndTime().toLocalDate().equals(java.time.LocalDate.now()))
                        .mapToDouble(session -> session.getTotalCost().doubleValue())
                        .sum();
            }

            return result != null ? ((Number) result).doubleValue() : 0.0;
        } catch (Exception e) {
            log.warn("Erreur lors du calcul du revenu d'aujourd'hui, utilisation de 0.0");
            return 0.0;
        }
    }
}