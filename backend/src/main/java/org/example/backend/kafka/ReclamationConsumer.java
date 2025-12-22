package org.example.backend.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.backend.entities.Reclamation;
import org.example.backend.repository.ReclamationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReclamationConsumer {

    private final ReclamationRepository reclamationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "reclamations", groupId = "parking-service-claims")
    public void consume(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            Reclamation rec = Reclamation.builder()
                    .userId(root.path("user").path("id").asText())
                    .userEmail(root.path("user").path("email").asText())
                    .userName(root.path("user").path("name").asText())
                    .userPhone(root.path("user").path("phone").asText())
                    .serviceType(root.path("claim").path("serviceType").asText())
                    .title(root.path("claim").path("title").asText())
                    .description(root.path("claim").path("description").asText())
                    .priority(root.path("claim").path("priority").asText())
                    .address(root.path("claim").path("location").path("address").asText())
                    .latitude(root.path("claim").path("location").path("latitude").asDouble())
                    .longitude(root.path("claim").path("location").path("longitude").asDouble())
                    .attachmentsJson(root.path("claim").path("attachments").toString())
                    .extraDataJson(root.path("claim").path("extraData").toString())
                    .receivedAt(LocalDateTime.now())
                    .build();

            reclamationRepository.save(rec);

            System.out.println("Saved reclamation id=" + rec.getId());

        } catch (Exception e) {
            System.err.println("Error parsing Kafka reclamation message: " + e.getMessage());
        }

        System.out.println("DEBUG: Received message: " + message);
    }
}

