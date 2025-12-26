package org.example.backend.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationRequestDTO {
    private Long spotId;
    private String driverId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}