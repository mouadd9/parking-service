package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.CheckInRequestDTO;
import org.example.backend.service.ParkingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;

    @PostMapping("/{id}/check-in")
    public ResponseEntity<String> checkIn(@PathVariable Long id, @RequestBody CheckInRequestDTO request) {

        // QUESTION: How do we secure this?
        // Currently, anyone can send any "userId" in the body.
        // TODO: Future integration with Clerk to get User ID from Principal/Token.

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
