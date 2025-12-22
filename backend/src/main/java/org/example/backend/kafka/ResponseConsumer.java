package org.example.backend.kafka;

// this file is to consume and store Kafka messages related to reclamation-responses that gonna be sent back to RECLAMATION Group
// the workflow is: US (producer) --> Kafka Topic "reclamation-responses" --> US (consumer) -> save to DB

import lombok.RequiredArgsConstructor;
import org.example.backend.entities.MessageResponse;
import org.example.backend.repository.MessageResponseRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

///// WILL BE ADAPTED ACORDING TO THE NEW DATABASE SCHEMA /////

@Component
@RequiredArgsConstructor
public class ResponseConsumer {

    private final MessageResponseRepository messageResponseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "reclamation-responses", groupId = "parking-service-claims")
    public void consume(String message) {

        // log raw for debugging
        // System.out.println("DEBUG: Received response message: " + message);

        JsonNode root;
        root = objectMapper.readTree(message);


        try {
            LocalDateTime ts = LocalDateTime.now();
            if (root.hasNonNull("timestamp")) {
                try {
                    ts = LocalDateTime.parse(root.path("timestamp").asText());
                } catch (Exception ignored) {
                }
            }

            // support both camelCase and snake_case keys
            long reclamationId = getLong(root, "claimId", "claim_id", "reclamationId", "reclamation_id");
            String operatorId = getText(root, "operatorId", "operator_id");
            String operatorName = getText(root, "operatorName", "operator_name");
            String responseMessage = getText(root, "responseMessage", "response_message", "responseMessage");

            if (reclamationId == -1L) {
                System.err.println("Missing numeric reclamation id in message, skipping save");
                return;
            }

            MessageResponse resp = MessageResponse.builder()
                    .reclamationId(reclamationId)
                    .operatorId(operatorId)
                    .operatorName(operatorName)
                    .responseMessage(responseMessage)
                    .createdAt(ts)
                    .build();

            messageResponseRepository.save(resp);

            System.out.println("Saved response id=" + resp.getId());

        } catch (Exception e) {
            System.err.println("Error parsing/saving response message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // helper methods to extract values with multiple possible keys
    // just making sure the data is extracted correctly regardless of the key naming convention used
    private long getLong(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.path(k);
            if (!v.isMissingNode() && !v.isNull()) {
                if (v.isNumber()) return v.asLong();
                try {
                    return Long.parseLong(v.asText());
                } catch (NumberFormatException ignored) {}
            }
        }
        return -1L;
    }

    private String getText(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.path(k);
            if (!v.isMissingNode() && !v.isNull()) {
                String t = v.asText();
                if (t != null && !t.isEmpty()) return t;
            }
        }
        return null;
    }

}
