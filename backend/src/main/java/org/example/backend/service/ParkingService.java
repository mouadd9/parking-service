package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.entities.ParkingSession;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.enums.SessionStatus;
import org.example.backend.repository.ParkingSessionRepository;
import org.example.backend.repository.ParkingSpotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ParkingService {
    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;

    @Transactional
    public void checkIn(Long spotId, String userId) {
        // 1. Fetch the spot from DB
        ParkingSpot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new RuntimeException("Spot not found with ID: " + spotId));

        // 2. Validation: Is the spot actually free?
        // Reminder: status = true means FREE
        if (Boolean.FALSE.equals(spot.getStatus())) {
            throw new IllegalStateException("Spot " + spot.getSpotNumber() + " is already occupied!");
        }

        // 3. Lock the Spot (Update status to Occupied)
        spot.setStatus(false); // false = Occupied
        spotRepository.save(spot);

        // 4. Create the Session
        ParkingSession session = ParkingSession.builder()
                .spot(spot)
                .driverId(userId) // Storing the ID provided by Controller
                .startTime(LocalDateTime.now())
                .status(SessionStatus.ACTIVE)
                .build();

        sessionRepository.save(session);

        System.out.println("DEBUG: Created Session ID " + session.getId() + " for User " + userId);
    }
}
