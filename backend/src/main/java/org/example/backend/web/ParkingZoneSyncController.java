package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.service.ParkingZoneSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parking-zones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParkingZoneSyncController {

    private final ParkingZoneSyncService syncService;

    @PostMapping("/sync")
    public ResponseEntity<?> syncParkingZones() {
        try {
            ParkingZoneSyncService.SyncResult result = syncService.syncParkingZonesFromOverpass();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Parking zones synced successfully",
                    "created", result.created(),
                    "updated", result.updated(),
                    "skipped", result.skipped(),
                    "total", result.total()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to sync parking zones: " + e.getMessage()
            ));
        }
    }
}
