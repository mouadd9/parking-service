package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "parking_spots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String spotNumber; // Ex: "P-101"

    @Column(nullable = false, unique = true)
    private String sensorId;   // Lien avec IoT (ex: "SENSOR-XYZ")

    private Boolean status;

    @Column(name = "hourly_rate", precision = 38, scale = 2)
    private BigDecimal hourlyRate;

    @ManyToOne
    @JoinColumn(name = "zone_id")
    private ParkingZone zone;
}
