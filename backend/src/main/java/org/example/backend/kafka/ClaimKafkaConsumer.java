package org.example.backend.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ClaimCreatedDto;
import org.example.backend.DTO.ClaimMessageDto;
import org.example.backend.service.ClaimService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j // Logger for logging information and errors Optional just to help in debugging
public class ClaimKafkaConsumer {

    private final ClaimService claimService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topic.claims:claims.rfm}", groupId = "${kafka.group-id:RFM}")
    public void consumeClaimMessage(String message) {
        log.info("========================================");
        log.info("RAW Kafka message received: {}", message);
        log.info("========================================");

        try {
            var jsonNode = objectMapper.readTree(message);
            String messageType = jsonNode.path("messageType").asText();

            log.info("Message type detected: {}", messageType);

            switch (messageType) {
                case "CLAIM_CREATED":
                    log.info("Handling CLAIM_CREATED...");
                    handleClaimCreated(message);
                    break;

                case "CLAIM_MESSAGE":
                    log.info("Handling CLAIM_MESSAGE...");
                    handleClaimMessage(message);
                    break;

                default:
                    log.warn("Unknown message type: {}", messageType);
            }

        } catch (Exception e) {
            log.error("ERROR processing Kafka message", e);
            e.printStackTrace();
        }
    }

    private void handleClaimCreated(String message) {
        try {
            log.info("Parsing CLAIM_CREATED DTO...");
            ClaimCreatedDto dto = objectMapper.readValue(message, ClaimCreatedDto.class);

            log.info("DTO parsed successfully. ClaimId: {}, ClaimNumber: {}",
                    dto.getClaimId(), dto.getClaimNumber());

            claimService.processClaimCreated(dto);

            log.info("SUCCESS: CLAIM_CREATED processed: {}", dto.getClaimNumber());
        } catch (Exception e) {
            log.error("ERROR handling CLAIM_CREATED", e);
            e.printStackTrace();
            throw new RuntimeException("Failed to process CLAIM_CREATED", e);
        }
    }

    private void handleClaimMessage(String message) {
        try {
            log.info("Parsing CLAIM_MESSAGE DTO...");
            ClaimMessageDto dto = objectMapper.readValue(message, ClaimMessageDto.class);

            log.info("DTO parsed successfully. ClaimId: {}", dto.getClaimId());

            claimService.processClaimMessage(dto);

            log.info("SUCCESS: CLAIM_MESSAGE processed for claim: {}", dto.getClaimNumber());
        } catch (Exception e) {
            log.error("ERROR handling CLAIM_MESSAGE", e);
            e.printStackTrace();
            throw new RuntimeException("Failed to process CLAIM_MESSAGE", e);
        }
    }

    // test listeners - to simulate the recieving Reclamation Group

    /**
     * TEST LISTENER - Simulates portal receiving your responses
     * This is just for testing - remove in production
     */
    @KafkaListener(topics = "${kafka.topic.responses:claims.responses}", groupId = "test-response-listener")
    public void testConsumeResponse(String message) {
        log.info("========================================");
        log.info("TEST: Response received by portal: {}", message);
        log.info("========================================");
    }

    /**
     * TEST LISTENER - Simulates portal receiving your status updates
     * This is just for testing - remove in production
     */
    @KafkaListener(topics = "${kafka.topic.status-updates:claims.status-updates}", groupId = "test-status-listener")
    public void testConsumeStatusUpdate(String message) {
        log.info("========================================");
        log.info("TEST: Status update received by portal: {}", message);
        log.info("========================================");
    }
}