package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String driverId; // ID venant de Clerk (User Token)

    @ManyToOne
    @JoinColumn(name = "spot_id")
    private ParkingSpot spot;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BigDecimal totalCost;



    @Enumerated(EnumType.STRING)
    private SessionStatus status;
}