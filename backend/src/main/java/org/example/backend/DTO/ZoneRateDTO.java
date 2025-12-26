package org.example.backend.DTO;



import lombok.Data;
import java.math.BigDecimal;

@Data
public class ZoneRateDTO {
    private Long id;
    private String name;
    private BigDecimal currentRate;
    private Integer capacity;
    private Long occupiedSpots;
    private BigDecimal averageDailyRevenue;

    // Optionnel : pour les propositions de nouveaux tarifs
    private BigDecimal proposedRate;
}