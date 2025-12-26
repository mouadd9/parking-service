package org.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingDetectionResponse {

    private String action;
    private Long spotId;
    private String spotNumber;
    private String zoneName;

    private Long sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String duration;
    private Double hourlyRate;
    private Double totalCost;

    private Boolean hasReservation;

    // ✅ AJOUT IMPORTANT
    private String driverId;

    private String spotStatus;
    private String message;
}
