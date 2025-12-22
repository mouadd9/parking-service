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
        return ParkingSpotDTO.builder()
                .id(entity.getId())
                .spotNumber(entity.getSpotNumber())
                .status(entity.getStatus())
                .build();
    }
}