package org.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingDetectionResponse {
    private String action;  // "entry_detected", "exit_detected", "error", "exit_ignored", "exit_corrected"
    private Long spotId;
    private String spotNumber;
    private String zoneName;
    private Long sessionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration;
    private Double hourlyRate;
    private Double totalCost;
    private boolean hasReservation;
    private String message;

    // Nouveau champ pour le statut du spot
    private String spotStatus; // "FREE", "OCCUPIED"
}