package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.SensorInfoDTO;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.repository.ParkingSpotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final ParkingSpotRepository spotRepository;

    public List<SensorInfoDTO> getAllSensors() {
        return spotRepository.findAll().stream()
                .map(this::convertToSensorInfoDTO)
                .collect(Collectors.toList());
    }

    public List<SensorInfoDTO> getSensorsByZone(Long zoneId) {
        return spotRepository.findByZoneId(zoneId).stream()
                .map(this::convertToSensorInfoDTO)
                .collect(Collectors.toList());
    }

    public List<SensorInfoDTO> getSensorsByStatus(String status) {
        boolean isFree = "FREE".equalsIgnoreCase(status) || "1".equals(status);
        return spotRepository.findAll().stream()
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
        return spotRepository.count();
    }

    private SensorInfoDTO convertToSensorInfoDTO(ParkingSpot spot) {
        SensorInfoDTO dto = new SensorInfoDTO();
        dto.setId(spot.getId());
        dto.setSensorId(spot.getSensorId());
        dto.setSpotNumber(spot.getSpotNumber());
        dto.setStatus(Boolean.TRUE.equals(spot.getStatus()) ? "FREE" : "OCCUPIED");
        dto.setZoneId(spot.getZone().getId());
        dto.setZoneName(spot.getZone().getName());
        dto.setLatitude(spot.getZone().getLatitude());
        dto.setLongitude(spot.getZone().getLongitude());
        dto.setCapacity(spot.getZone().getCapacity());
        dto.setHourlyRate(spot.getZone().getHourlyRate());
        return dto;
    }
}