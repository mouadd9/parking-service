package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.SensorInfoDTO;
import org.example.backend.service.SensorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;

    @GetMapping
    public ResponseEntity<List<SensorInfoDTO>> getAllSensors() {
        return ResponseEntity.ok(sensorService.getAllSensors());
    }

    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<List<SensorInfoDTO>> getSensorsByZone(@PathVariable Long zoneId) {
        return ResponseEntity.ok(sensorService.getSensorsByZone(zoneId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SensorInfoDTO>> getSensorsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(sensorService.getSensorsByStatus(status));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalSensors() {
        return ResponseEntity.ok(sensorService.getTotalSensorsCount());
    }
}