package org.example.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.backend.entities.ParkingSession;
import org.example.backend.entities.ParkingSpot;
import org.example.backend.enums.SessionStatus;
import org.example.backend.repository.ParkingSessionRepository;
import org.example.backend.repository.ParkingSpotRepository;
import org.example.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingService {
    private final ParkingSpotRepository spotRepository;
    private final ParkingSessionRepository sessionRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public void checkIn(Long spotId, String clerkUserId) {

        var user = utilisateurRepository.findByClerkId(clerkUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur inconnu ! Avez-vous créé un compte via Clerk ?"));

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
                .driverId(user.getClerkId()) // On est sûr que c'est un ID valide
                .startTime(LocalDateTime.now())
                .status(SessionStatus.ACTIVE)
                .build();

        sessionRepository.save(session);

        System.out.println("DEBUG: Created Session ID " + session.getId() + " for User " + clerkUserId);
    }

    // Affichage "Top of Map" ---
    public ParkingSession getActiveSession(String userId) {
        return sessionRepository.findByDriverIdAndStatus(userId, SessionStatus.ACTIVE)
                .orElse(null);
    }

    // POUR LE FRONTEND : Historique ---
    public List<ParkingSession> getUserHistory(String userId) {
        return sessionRepository.findByDriverId(userId);
    }

    // --- POUR LE FRONTEND : Bouton "Terminer" ---
    @Transactional
    public void checkOutManual(String userId) {

        // 1. Trouver la session active du conducteur
        ParkingSession session = sessionRepository.findByDriverIdAndStatus(userId, SessionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aucune session active trouvée pour cet utilisateur."));

        // 2. Calculer le prix (Logique standard)
        LocalDateTime endTime = LocalDateTime.now();
        session.setEndTime(endTime);
        session.setStatus(SessionStatus.COMPLETED);

        // Exemple tarif: 10 DHS / heure (à adapter selon votre entité Zone)
        BigDecimal hourlyRate = session.getSpot().getZone().getHourlyRate();
        if (hourlyRate == null) hourlyRate = BigDecimal.TEN; // Fallback

        long minutes = Duration.between(session.getStartTime(), endTime).toMinutes();
        double hours = Math.max(minutes / 60.0, 0.05); // Minimum facturé

        session.setTotalCost(hourlyRate.multiply(BigDecimal.valueOf(hours)));

        // 3. Libérer la place
        ParkingSpot spot = session.getSpot();
        spot.setStatus(true); // LIBRE
        spotRepository.save(spot);

        sessionRepository.save(session);
    }

}
