package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParkingZoneDTO {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
    private BigDecimal hourlyRate;
    // Note : On ne met PAS la liste des spots ici.
    // La carte n'en a pas besoin pour l'affichage global.
}