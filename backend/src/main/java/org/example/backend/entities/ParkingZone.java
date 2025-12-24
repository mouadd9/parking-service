package org.example.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "parking_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private BigDecimal hourlyRate; // Tarif par heure

    // pour afficher sur la carte
    private Double latitude;
    private Double longitude;
    private Integer capacity;
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // Empêche Lombok de faire une boucle infinie
    @JsonIgnore       // Empêche l'API de renvoyer la liste complète (trop lourd) si on demande juste la zone
    private List<ParkingSpot> spots;
}
