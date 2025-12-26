package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entities.ParkingZone;
import org.example.backend.repository.ParkingZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingZoneSyncService {

    private final OverpassService overpassService;
    private final ParkingZoneRepository parkingZoneRepository;

    // Default values when not found in Overpass data
    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("10.00");
    private static final Integer DEFAULT_CAPACITY = 35;

    @Transactional
    public SyncResult syncParkingZonesFromOverpass() {
        log.info("Starting parking zones sync from Overpass API");

        try {
            List<OverpassService.ParkingData> parkingDataList = overpassService.fetchTetouanParkings();

            int created = 0;
            int updated = 0;
            int skipped = 0;

            for (OverpassService.ParkingData data : parkingDataList) {
                try {
                    // Check if parking zone already exists by name
                    Optional<ParkingZone> existingOpt = parkingZoneRepository.findByName(data.getName());

                    if (existingOpt.isPresent()) {
                        // Update existing zone
                        ParkingZone existing = existingOpt.get();
                        boolean changed = false;

                        // Update coordinates if they changed
                        if (!existing.getLatitude().equals(data.getLatitude()) ||
                            !existing.getLongitude().equals(data.getLongitude())) {
                            existing.setLatitude(data.getLatitude());
                            existing.setLongitude(data.getLongitude());
                            changed = true;
                        }

                        // Update capacity if provided and different
                        if (data.getCapacity() != null && !data.getCapacity().equals(existing.getCapacity())) {
                            existing.setCapacity(data.getCapacity());
                            changed = true;
                        }

                        // Update hourly rate if provided and different
                        if (data.getHourlyRate() != null && !data.getHourlyRate().equals(existing.getHourlyRate())) {
                            existing.setHourlyRate(data.getHourlyRate());
                            changed = true;
                        }

                        if (changed) {
                            parkingZoneRepository.save(existing);
                            updated++;
                            log.debug("Updated parking zone: {}", data.getName());
                        } else {
                            skipped++;
                        }
                    } else {
                        // Create new parking zone
                        ParkingZone newZone = ParkingZone.builder()
                                .name(data.getName())
                                .latitude(data.getLatitude())
                                .longitude(data.getLongitude())
                                .hourlyRate(data.getHourlyRate() != null ? data.getHourlyRate() : DEFAULT_HOURLY_RATE)
                                .capacity(data.getCapacity() != null ? data.getCapacity() : DEFAULT_CAPACITY)
                                .build();

                        parkingZoneRepository.save(newZone);
                        created++;
                        log.debug("Created new parking zone: {}", data.getName());
                    }
                } catch (Exception e) {
                    log.error("Failed to process parking zone {}: {}", data.getName(), e.getMessage());
                    skipped++;
                }
            }

            log.info("Sync completed: {} created, {} updated, {} skipped", created, updated, skipped);

            return new SyncResult(created, updated, skipped, parkingDataList.size());
        } catch (Exception e) {
            log.error("Parking zones sync failed", e);
            throw new RuntimeException("Failed to sync parking zones from Overpass API", e);
        }
    }

    public record SyncResult(int created, int updated, int skipped, int total) {}
}
