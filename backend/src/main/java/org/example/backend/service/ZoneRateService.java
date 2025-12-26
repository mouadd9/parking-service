package org.example.backend.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ZoneRateDTO;
import org.example.backend.entities.ParkingZone;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.entities.ParkingSession;
import org.example.backend.repository.ParkingZoneRepository;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ParkingSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneRateService {

    private final ParkingZoneRepository parkingZoneRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingSessionRepository parkingSessionRepository;

    public List<ZoneRateDTO> getAllZoneRates() {
        log.info("Récupération de tous les tarifs de zone...");

        List<ParkingZone> zones = parkingZoneRepository.findAll();
        List<ZoneRateDTO> zoneRates = new ArrayList<>();

        for (ParkingZone zone : zones) {
            try {
                ZoneRateDTO dto = convertToZoneRateDTO(zone);
                zoneRates.add(dto);
            } catch (Exception e) {
                log.error("Erreur lors de la conversion de la zone {}: {}", zone.getId(), e.getMessage());
            }
        }

        log.info("{} tarifs de zone récupérés", zoneRates.size());
        return zoneRates;
    }

    @Transactional
    public boolean updateZoneRate(Long zoneId, BigDecimal newRate) {
        log.info("Mise à jour du tarif de la zone {} à {}", zoneId, newRate);

        Optional<ParkingZone> zoneOpt = parkingZoneRepository.findById(zoneId);

        if (zoneOpt.isPresent()) {
            ParkingZone zone = zoneOpt.get();
            BigDecimal oldRate = zone.getHourlyRate();
            zone.setHourlyRate(newRate);
            parkingZoneRepository.save(zone);

            log.info("✅ Tarif mis à jour: zone {} de {} à {}", zoneId, oldRate, newRate);
            return true;
        }

        log.warn("Zone non trouvée avec l'ID: {}", zoneId);
        return false;
    }

    public ZoneRateDTO getZoneRateById(Long zoneId) {
        Optional<ParkingZone> zoneOpt = parkingZoneRepository.findById(zoneId);

        if (zoneOpt.isPresent()) {
            return convertToZoneRateDTO(zoneOpt.get());
        }

        return null;
    }

    private ZoneRateDTO convertToZoneRateDTO(ParkingZone zone) {
        ZoneRateDTO dto = new ZoneRateDTO();
        dto.setId(zone.getId());
        dto.setName(zone.getName());
        dto.setCurrentRate(zone.getHourlyRate());
        dto.setCapacity(zone.getCapacity());

        // Calculer les spots occupés dans cette zone
        Long occupiedSpots = calculateOccupiedSpots(zone.getId());
        dto.setOccupiedSpots(occupiedSpots);

        // Calculer le revenu moyen quotidien
        BigDecimal averageDailyRevenue = calculateAverageDailyRevenue(zone.getId());
        dto.setAverageDailyRevenue(averageDailyRevenue);

        return dto;
    }

    private Long calculateOccupiedSpots(Long zoneId) {
        try {
            // Méthode 1: Utiliser le repository si la méthode existe
            return parkingSpotRepository.countByZoneIdAndStatus(zoneId, false);
        } catch (Exception e) {
            // Méthode 2: Calcul manuel
            log.warn("Méthode countByZoneIdAndStatus non disponible, calcul manuel");
            List<ParkingSpot> zoneSpots = parkingSpotRepository.findByZoneId(zoneId);
            return zoneSpots.stream()
                    .filter(spot -> Boolean.FALSE.equals(spot.getStatus()))
                    .count();
        }
    }

    private BigDecimal calculateAverageDailyRevenue(Long zoneId) {
        try {
            // Récupérer les sessions des 30 derniers jours
            LocalDateTime startDate = LocalDateTime.now().minusDays(30);
            LocalDateTime endDate = LocalDateTime.now();

            // Méthode 1: Utiliser une requête personnalisée si elle existe
            BigDecimal totalRevenue = parkingSessionRepository.calculateRevenueForZone(zoneId, startDate, endDate);

            if (totalRevenue != null) {
                // Diviser par 30 pour obtenir la moyenne quotidienne
                return totalRevenue.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
            }

            // Méthode 2: Calcul manuel
            List<ParkingSession> zoneSessions = parkingSessionRepository.findAll().stream()
                    .filter(session -> {
                        if (session.getSpot() == null || session.getSpot().getZone() == null) {
                            return false;
                        }
                        return session.getSpot().getZone().getId().equals(zoneId) &&
                                session.getEndTime() != null &&
                                session.getEndTime().isAfter(startDate) &&
                                session.getEndTime().isBefore(endDate) &&
                                session.getTotalCost() != null;
                    })
                    .collect(Collectors.toList());

            BigDecimal manualTotal = zoneSessions.stream()
                    .map(ParkingSession::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return manualTotal.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            log.warn("Erreur lors du calcul du revenu moyen: {}", e.getMessage());
            // Estimation basée sur l'occupation et le tarif
            Optional<ParkingZone> zoneOpt = parkingZoneRepository.findById(zoneId);
            if (zoneOpt.isPresent()) {
                ParkingZone zone = zoneOpt.get();
                Long occupiedSpots = calculateOccupiedSpots(zoneId);

                // Estimation: (tarif horaire * 8 heures * spots occupés) / 2 (car tous ne sont pas occupés toute la journée)
                BigDecimal estimatedDaily = zone.getHourlyRate()
                        .multiply(BigDecimal.valueOf(8))
                        .multiply(BigDecimal.valueOf(occupiedSpots))
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

                return estimatedDaily;
            }

            return BigDecimal.ZERO;
        }
    }

    // Méthode pour les données de test (développement uniquement)
    public List<ZoneRateDTO> getMockZoneRates() {
        log.info("Génération de données de test pour les tarifs de zone");

        List<ZoneRateDTO> mockRates = new ArrayList<>();

        // Données de test
        mockRates.add(createMockZoneRate(1L, "Zone A - Centre", 5.0, 100, 75, 1200.0));
        mockRates.add(createMockZoneRate(2L, "Zone B - Commerciale", 4.5, 80, 60, 850.0));
        mockRates.add(createMockZoneRate(3L, "Zone C - Résidentielle", 3.5, 120, 90, 950.0));
        mockRates.add(createMockZoneRate(4L, "Zone D - Périphérie", 2.5, 150, 50, 400.0));
        mockRates.add(createMockZoneRate(5L, "Zone E - Aéroport", 7.0, 200, 150, 2800.0));

        return mockRates;
    }

    private ZoneRateDTO createMockZoneRate(Long id, String name, double rate, int capacity,
                                           long occupiedSpots, double dailyRevenue) {
        ZoneRateDTO dto = new ZoneRateDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setCurrentRate(BigDecimal.valueOf(rate));
        dto.setCapacity(capacity);
        dto.setOccupiedSpots(occupiedSpots);
        dto.setAverageDailyRevenue(BigDecimal.valueOf(dailyRevenue));
        return dto;
    }
}