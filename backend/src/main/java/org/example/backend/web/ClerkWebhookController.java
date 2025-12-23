package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import org.example.backend.service.UserServiceClerkWebhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class ClerkWebhookController {

    private final UserServiceClerkWebhook webhookService;

    @PostMapping("/api/webhooks/clerk-user")
    public ResponseEntity<String> clerkUserWebhook(@RequestBody String payload) throws Exception {
        webhookService.handle(payload);
        return ResponseEntity.ok("ok");
    }
}
