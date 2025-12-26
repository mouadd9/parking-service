package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.ReservationRequestDTO;
import org.example.backend.DTO.ReservationResponseDTO;
import org.example.backend.service.ReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/create")
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequestDTO request) {
        try {
            ReservationResponseDTO response = reservationService.createReservation(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getReservationStatus(@PathVariable Long id) {
        try {
            ReservationResponseDTO response = reservationService.getReservationStatus(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/user/{driverId}")
    public ResponseEntity<?> getUserReservations(@PathVariable String driverId) {
        try {
            List<ReservationResponseDTO> reservations = reservationService.getUserReservations(driverId);
            return ResponseEntity.ok(reservations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            reservationService.cancelReservation(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Réservation annulée avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", e.getMessage()
            ));
        }
    }
}