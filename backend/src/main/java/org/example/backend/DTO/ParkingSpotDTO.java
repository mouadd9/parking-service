package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ParkingSpotDTO {
    private Long id;
    private String spotNumber; // Ex: "P-101"
    private String sensorId;   // Ex: "SENSOR-001"
    private Boolean status;    // true = Libre, false = Occupé
    private ZoneInfo zone;     // Zone information

    @Data
    @Builder
    public static class ZoneInfo {
        private Long id;
        private String name;
        private BigDecimal hourlyRate;
    }
}