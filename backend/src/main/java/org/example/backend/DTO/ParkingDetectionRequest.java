package org.example.backend.DTO;

import lombok.Data;

@Data
public class ParkingDetectionRequest {
    private String sensorId;
    private String status; // "occupied" ou "free"
    private String timestamp;
}