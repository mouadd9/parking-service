package org.example.backend.DTO;

import lombok.Builder;
import lombok.Data;
import org.example.backend.entities.Reservation;

@Data
@Builder
public class ParkingDetectionResponse {
    private String action;
    private Long spotId;
    private String spotNumber;
    private String zoneName;
    private Long sessionId;
    private boolean hasReservation;
    private Reservation reservation;
    private String driverId;  // Ajout de ce champ
    private String duration;
    private Double totalCost;
    private String message;
}