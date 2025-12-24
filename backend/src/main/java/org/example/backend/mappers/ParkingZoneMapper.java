package org.example.backend.mappers;

import org.example.backend.DTO.ParkingZoneDTO;
import org.example.backend.entities.ParkingZone;
import org.springframework.stereotype.Service;

@Service
public class ParkingZoneMapper {

    public ParkingZoneDTO toDTO(ParkingZone entity) {
        if (entity == null) {
            return null;
        }
        return ParkingZoneDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .hourlyRate(entity.getHourlyRate())
                .capacity(entity.getCapacity())
                .build();
    }
}