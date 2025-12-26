package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.service.ParkingStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingStatusController {

    private final ParkingStatusService statusService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getParkingStatus() {
        Map<String, Object> status = statusService.getParkingStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/spots/status")
    public ResponseEntity<?> getAllSpotsStatus() {
        return ResponseEntity.ok(statusService.getAllSpotsStatus());
    }
}