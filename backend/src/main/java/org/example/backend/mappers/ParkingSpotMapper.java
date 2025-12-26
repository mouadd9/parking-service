package org.example.backend.mappers;

import org.example.backend.DTO.ParkingSpotDTO;
import org.example.backend.entities.ParkingSpot;
import org.springframework.stereotype.Service;

@Service
public class ParkingSpotMapper {
    public ParkingSpotDTO toDTO(ParkingSpot entity) {
        if (entity == null) {
            return null;
        }

        // Build zone info
        ParkingSpotDTO.ZoneInfo zoneInfo = null;
        if (entity.getZone() != null) {
            zoneInfo = ParkingSpotDTO.ZoneInfo.builder()
                    .id(entity.getZone().getId())
                    .name(entity.getZone().getName())
                    .hourlyRate(entity.getZone().getHourlyRate())
                    .build();
        }

        return ParkingSpotDTO.builder()
                .id(entity.getId())
                .spotNumber(entity.getSpotNumber())
                .sensorId(entity.getSensorId())
                .status(entity.getStatus())
                .zone(zoneInfo)
                .build();
    }
}