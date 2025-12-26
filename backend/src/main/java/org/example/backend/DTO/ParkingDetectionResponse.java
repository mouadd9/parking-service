package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkingDetectionResponse {
    private String action; // "entry_detected" ou "exit_detected"
    private Long spotId;
    private String spotNumber;
    private String zoneName;
    private Long sessionId;
    private Boolean hasReservation;
    private Object reservation;
    private String duration;
    private Double totalCost;
}