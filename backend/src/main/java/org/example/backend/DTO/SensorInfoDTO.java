package org.example.backend.DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SensorInfoDTO {
    private Long id;
    private String sensorId;
    private String spotNumber;
    private String status; // "FREE" ou "OCCUPIED"
    private Long zoneId;
    private String zoneName;
    private Double latitude;
    private Double longitude;
    private Integer capacity;
    private BigDecimal hourlyRate;
}