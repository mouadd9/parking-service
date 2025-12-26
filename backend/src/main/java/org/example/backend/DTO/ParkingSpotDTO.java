package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkingSpotDTO {
    private Long id;
    private String spotNumber; // Ex: "P-101"
    private Boolean status;

    private Long durationMinutes;
    private Double totalCost;// true = Libre, false = Occupé

}