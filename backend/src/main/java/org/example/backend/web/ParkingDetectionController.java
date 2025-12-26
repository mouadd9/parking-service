package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.ParkingDetectionRequest;
import org.example.backend.DTO.ParkingDetectionResponse;
import org.example.backend.service.ParkingDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
public class ParkingDetectionController {

    private final ParkingDetectionService detectionService;

    @PostMapping("/detect")
    public ResponseEntity<ParkingDetectionResponse> handleParkingDetection(
            @RequestBody ParkingDetectionRequest request) {
        try {
            ParkingDetectionResponse response = detectionService.handleDetection(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Créer une réponse d'erreur
            ParkingDetectionResponse errorResponse = ParkingDetectionResponse.builder()
                    .action("error")
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}