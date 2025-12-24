package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkingSpotDTO {
    private Long id;
    private String spotNumber; // Ex: "P-101"
    private Boolean status;    // true = Libre, false = Occupé

}