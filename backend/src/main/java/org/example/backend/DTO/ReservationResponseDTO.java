package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservationResponseDTO {
    private Long id;
    private Long spotId;
    private String spotNumber;
    private String driverId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}