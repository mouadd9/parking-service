package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.ParkingSpotDTO;
import org.example.backend.DTO.ParkingZoneDTO;
import org.example.backend.service.ParkingZoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParkingZoneController {

    private final ParkingZoneService service;

    @GetMapping
    public ResponseEntity<List<ParkingZoneDTO>> getAllZones() {
        return ResponseEntity.ok(service.getAllZones());
    }

    @GetMapping("/{id}/spots")
    public ResponseEntity<List<ParkingSpotDTO>> getSpotsByZone(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSpotsByZone(id));
    }

}
