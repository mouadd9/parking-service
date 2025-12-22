package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.example.backend.entities.Utilisateur;
import org.example.backend.enums.Role;
import org.example.backend.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserServiceClerkWebhook {

    private final UtilisateurRepository userRepository;
    private final ClerkClientService clerkClientService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public void handle(String payload) throws Exception {
        JsonNode root = mapper.readTree(payload);

        String type = root.path("type").asText("");
        JsonNode data = root.path("data");

        String clerkId = data.path("id").asText(null);
        String firstName = data.path("first_name").asText("");
        String lastName = data.path("last_name").asText("");

        if (clerkId == null || clerkId.isBlank()) {
            throw new IllegalArgumentException("Webhook Clerk: id manquant");
        }

        // ✅ 1) Logique CREATE (inchangée)
        if ("user.created".equals(type)) {

            if (userRepository.existsByClerkId(clerkId)) return;

            String email = extractPrimaryEmailFromWebhook(data);

            if (email == null || email.isBlank()) {
                email = clerkClientService.fetchPrimaryEmailFromClerk(clerkId);
            }
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Email introuvable (webhook + fetch Clerk)");
            }
            if (userRepository.existsByEmail(email)) return;

            Utilisateur u = new Utilisateur();
            u.setClerkId(clerkId);
            u.setEmail(email);

            String username = (firstName + " " + lastName).trim();
            if (username.isBlank()) {
                String suffix = clerkId.length() >= 6 ? clerkId.substring(clerkId.length() - 6) : clerkId;
                username = "User " + suffix;
            }
            u.setNom(username);

            u.setRole(Role.CONDUCTEUR);
            u.setMotDePasse(null);
            userRepository.save(u);
            return;
        }

        // ✅ 2) Logique UPDATE (nouvelle) : seulement si user existe déjà
        if ("user.updated".equals(type)) {
            Utilisateur u = userRepository.findByClerkId(clerkId).orElse(null);
            if (u == null) return; // <-- comme demandé : update seulement si clerk_id existe

            // Update username (first + last)
            String username = (firstName + " " + lastName).trim();
            if (username.isBlank()) {
                // on ne force pas un "User xxxx" si déjà un username dans la DB
                // mais si tu veux, tu peux garder un fallback
                username = u.getNom();
            }
            if (username != null && !username.isBlank()) {
                u.setNom(username);
            }

            // Update email primaire si changé
            String email = extractPrimaryEmailFromWebhook(data);
            if (email == null || email.isBlank()) {
                email = clerkClientService.fetchPrimaryEmailFromClerk(clerkId);
            }

            if (email != null && !email.isBlank()) {
                // éviter collision avec un autre user
                boolean usedByAnother = userRepository.existsByEmail(email) && !email.equalsIgnoreCase(u.getEmail());
                if (!usedByAnother) {
                    u.setEmail(email);
                } else {
                    log.warn("Email {} déjà utilisé par un autre user, update ignoré pour clerkId={}", email, clerkId);
                }
            }

            // IMPORTANT: on ne touche pas role/CIN/MATRICULE/password
            userRepository.save(u);
            return;
        }

        // autres events -> ignore
    }

    private String extractPrimaryEmailFromWebhook(JsonNode data) {
        String primaryId = data.path("primary_email_address_id").asText(null);
        if (primaryId == null) return null;

        for (JsonNode e : data.path("email_addresses")) {
            if (primaryId.equals(e.path("id").asText())) {
                return e.path("email_address").asText(null);
            }
        }
        return null;
    }
}
