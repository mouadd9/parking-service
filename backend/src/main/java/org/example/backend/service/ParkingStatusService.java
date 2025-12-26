package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ParkingSessionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParkingStatusService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;

    public Map<String, Object> getParkingStatus() {
        List<ParkingSpot> allSpots = spotRepository.findAll();

        long totalSpots = allSpots.size();
        long occupiedSpots = allSpots.stream().filter(spot -> Boolean.FALSE.equals(spot.getStatus())).count();
        long freeSpots = totalSpots - occupiedSpots;

        Map<String, Object> status = new HashMap<>();
        status.put("totalSpots", totalSpots);
        status.put("occupiedSpots", occupiedSpots);
        status.put("freeSpots", freeSpots);
        status.put("occupationRate", String.format("%.2f%%", (occupiedSpots * 100.0 / totalSpots)));

        // Statistiques par zone
        Map<Long, Map<String, Object>> zonesStatus = new HashMap<>();
        allSpots.stream()
                .collect(java.util.stream.Collectors.groupingBy(spot -> spot.getZone().getId()))
                .forEach((zoneId, spots) -> {
                    long zoneTotal = spots.size();
                    long zoneOccupied = spots.stream().filter(spot -> Boolean.FALSE.equals(spot.getStatus())).count();

                    Map<String, Object> zoneStatus = new HashMap<>();
                    zoneStatus.put("zoneName", spots.get(0).getZone().getName());
                    zoneStatus.put("totalSpots", zoneTotal);
                    zoneStatus.put("occupiedSpots", zoneOccupied);
                    zoneStatus.put("freeSpots", zoneTotal - zoneOccupied);
                    zoneStatus.put("occupationRate", String.format("%.2f%%", (zoneOccupied * 100.0 / zoneTotal)));

                    zonesStatus.put(zoneId, zoneStatus);
                });

        status.put("zones", zonesStatus);

        return status;
    }

    public List<Map<String, Object>> getAllSpotsStatus() {
        List<ParkingSpot> spots = spotRepository.findAll();

        return spots.stream()
                .map(spot -> {
                    Map<String, Object> spotStatus = new HashMap<>();
                    spotStatus.put("spotId", spot.getId());
                    spotStatus.put("spotNumber", spot.getSpotNumber());
                    spotStatus.put("sensorId", spot.getSensorId());
                    spotStatus.put("status", Boolean.TRUE.equals(spot.getStatus()) ? "FREE" : "OCCUPIED");
                    spotStatus.put("zoneId", spot.getZone().getId());
                    spotStatus.put("zoneName", spot.getZone().getName());
                    spotStatus.put("latitude", spot.getZone().getLatitude());
                    spotStatus.put("longitude", spot.getZone().getLongitude());

                    // Vérifier s'il y a une réservation active
                    // Vous pouvez ajouter cette logique si nécessaire

                    return spotStatus;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}