package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.CheckInRequestDTO;
import org.example.backend.entities.ParkingSession;
import org.example.backend.service.ParkingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    // this will return the current active parking session for a user (it should be one active)
    // URL : GET /api/my-active-session?userId=user_123
    @GetMapping("/my-active-session")
    public ResponseEntity<ParkingSession> getActiveSession(@RequestParam String userId) {
        ParkingSession session = parkingService.getActiveSession(userId);
        if (session == null) {
            return ResponseEntity.noContent().build(); // 204 No Content (Pas de session)
        }
        return ResponseEntity.ok(session);
    }

    // this will be called when the driver wants to manually end his parking session
    // URL : POST /api/check-out?userId=user_123
    @PostMapping("/check-out")
    public ResponseEntity<String> checkOut(@RequestParam String userId) {
        try {
            parkingService.checkOutManual(userId);
            return ResponseEntity.ok("Session terminée. Place libérée !");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }

    @PostMapping("/{id}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable Long id, @RequestBody CheckInRequestDTO request) {

        String userId = request.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: User ID is required.");
        }

        try {
            parkingService.checkIn(id, userId);
            return ResponseEntity.ok("Check-in successful. Drive safely!");
        } catch (IllegalStateException e) {
            // Returns 409 Conflict if spot is already taken
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
