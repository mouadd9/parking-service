package org.example.backend.config;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.entities.ParkingZone;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.ParkingZoneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ParkingZoneRepository zoneRepository, ParkingSpotRepository parkingSpotRepository) {
        return args -> {
            if (zoneRepository.count() == 0) {

                // 1. ZONES
                ParkingZone zone1 = ParkingZone.builder()
                        .name("Centre Ville")
                        .latitude(35.5711).longitude(-5.3724).hourlyRate(new BigDecimal("5.00"))
                        .build();

                ParkingZone zone2 = ParkingZone.builder()
                        .name("Plage Martil")
                        .latitude(35.6231).longitude(-5.2750).hourlyRate(new BigDecimal("3.00"))
                        .build();

                zoneRepository.saveAll(List.of(zone1, zone2));

                // 2. PLACES (true = Libre, false = Occupé)

                List<ParkingSpot> spotsZone1 = List.of(
                        ParkingSpot.builder().spotNumber("P-101").sensorId("S-01").status(true).zone(zone1).build(),
                        ParkingSpot.builder().spotNumber("P-102").sensorId("S-02").status(false).zone(zone1).build(),
                        ParkingSpot.builder().spotNumber("P-103").sensorId("S-03").status(true).zone(zone1).build()
                );

                List<ParkingSpot> spotsZone2 = List.of(
                        ParkingSpot.builder().spotNumber("M-01").sensorId("S-10").status(true).zone(zone2).build(),
                        ParkingSpot.builder().spotNumber("M-02").sensorId("S-11").status(true).zone(zone2).build()
                );

                parkingSpotRepository.saveAll(spotsZone1);
                parkingSpotRepository.saveAll(spotsZone2);
            }
        };
    }
}