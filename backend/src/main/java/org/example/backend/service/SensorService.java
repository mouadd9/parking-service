package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.SensorInfoDTO;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.repository.ParkingSpotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorService {

    private final ParkingSpotRepository spotRepository;

    public List<SensorInfoDTO> getAllSensors() {
        log.info("Récupération de tous les capteurs...");
        List<ParkingSpot> allSpots = spotRepository.findAll();
        log.info("Nombre total de spots trouvés: {}", allSpots.size());

        List<SensorInfoDTO> sensors = allSpots.stream()
                .filter(spot -> spot.getSensorId() != null && !spot.getSensorId().isEmpty())
                .map(this::convertToSensorInfoDTO)
                .collect(Collectors.toList());

        log.info("Nombre de capteurs avec sensorId non nul: {}", sensors.size());

        if (sensors.isEmpty()) {
            log.warn("⚠️ Aucun capteur avec sensorId trouvé dans la base de données!");

            // Créer des capteurs de test si aucun n'existe
            sensors = createTestSensors();
        }

        return sensors;
    }

    public List<SensorInfoDTO> getSensorsByZone(Long zoneId) {
        return spotRepository.findByZoneId(zoneId).stream()
                .filter(spot -> spot.getSensorId() != null)
                .map(this::convertToSensorInfoDTO)
                .collect(Collectors.toList());
    }

    public List<SensorInfoDTO> getSensorsByStatus(String status) {
        boolean isFree = "FREE".equalsIgnoreCase(status);

        return spotRepository.findAll().stream()
                .filter(spot -> spot.getSensorId() != null)
                .filter(spot -> {
                    if (isFree) {
                        return Boolean.TRUE.equals(spot.getStatus());
                    } else {
                        return Boolean.FALSE.equals(spot.getStatus());
                    }
                })
                .map(this::convertToSensorInfoDTO)
                .collect(Collectors.toList());
    }

    public Long getTotalSensorsCount() {
        return spotRepository.countBySensorIdIsNotNull();
    }

    private SensorInfoDTO convertToSensorInfoDTO(ParkingSpot spot) {
        try {
            SensorInfoDTO dto = new SensorInfoDTO();
            dto.setId(spot.getId());
            dto.setSensorId(spot.getSensorId());
            dto.setSpotNumber(spot.getSpotNumber());
            dto.setStatus(Boolean.TRUE.equals(spot.getStatus()) ? "FREE" : "OCCUPIED");

            if (spot.getZone() != null) {
                dto.setZoneId(spot.getZone().getId());
                dto.setZoneName(spot.getZone().getName());
                dto.setLatitude(spot.getZone().getLatitude());
                dto.setLongitude(spot.getZone().getLongitude());
                dto.setCapacity(spot.getZone().getCapacity());
                dto.setHourlyRate(spot.getZone().getHourlyRate());
            }

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de la conversion du spot {}", spot.getId(), e);
            return null;
        }
    }

    private List<SensorInfoDTO> createTestSensors() {
        log.info("Création de capteurs de test...");
        List<SensorInfoDTO> testSensors = List.of(
                createTestSensor("sensor_001", "A-01", 1L, "Zone A", 48.8566, 2.3522),
                createTestSensor("sensor_002", "A-02", 1L, "Zone A", 48.8566, 2.3522),
                createTestSensor("sensor_003", "B-01", 2L, "Zone B", 48.8584, 2.2945),
                createTestSensor("sensor_004", "B-02", 2L, "Zone B", 48.8584, 2.2945),
                createTestSensor("sensor_005", "C-01", 3L, "Zone C", 48.8600, 2.3370)
        );

        log.info("{} capteurs de test créés", testSensors.size());
        return testSensors;
    }

    private SensorInfoDTO createTestSensor(String sensorId, String spotNumber, Long zoneId,
                                           String zoneName, Double latitude, Double longitude) {
        SensorInfoDTO dto = new SensorInfoDTO();
        dto.setId(zoneId * 100L + Integer.parseInt(spotNumber.split("-")[1]));
        dto.setSensorId(sensorId);
        dto.setSpotNumber(spotNumber);
        dto.setStatus("FREE");
        dto.setZoneId(zoneId);
        dto.setZoneName(zoneName);
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        dto.setCapacity(50);
        dto.setHourlyRate(BigDecimal.valueOf(5.0));
        return dto;
    }
}