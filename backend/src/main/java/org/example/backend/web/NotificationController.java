package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.DTO.NotificationRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequestDTO request) {
        // Pour l'instant, on simule l'envoi de notification
        // Vous pouvez intégrer avec un service d'email/SMS/push plus tard

        System.out.println("Notification envoyée: " + request.getType());
        System.out.println("À: " + request.getUserId());
        System.out.println("Titre: " + request.getTitle());
        System.out.println("Message: " + request.getMessage());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification envoyée avec succès"
        ));
    }
}