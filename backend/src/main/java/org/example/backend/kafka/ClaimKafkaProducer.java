package org.example.backend.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ServiceResponseDto;
import org.example.backend.DTO.StatusUpdateDto;
import org.example.backend.entities.Claim;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j // for debugging and logging
public class ClaimKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.responses:claims.responses}")
    private String responseTopic;

    @Value("${kafka.topic.status-updates:claims.status-updates}")
    private String statusUpdateTopic;

    public void sendServiceResponse(Claim claim, String operatorId, String operatorName,
                                    String responseMessage, String serviceReference,
                                    List<ServiceResponseDto.AttachmentDto> attachments) {
        try {
            ServiceResponseDto dto = ServiceResponseDto.builder()
                    .messageId(UUID.randomUUID().toString())
                    .messageType("SERVICE_RESPONSE")
                    .timestamp(Instant.now().toString())
                    .version("1.0")
                    .claimId(claim.getClaimUuid())
                    .claimNumber(claim.getClaimNumber())
                    .correlationId(claim.getCorrelationUuid())
                    .response(ServiceResponseDto.ResponseDto.builder()
                            .from(ServiceResponseDto.FromDto.builder()
                                    .serviceType(claim.getServiceType())
                                    .operatorId(operatorId)
                                    .operatorName(operatorName)
                                    .build())
                            .message(responseMessage)
                            .attachments(attachments)
                            .serviceReference(serviceReference)
                            .build())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send(responseTopic, claim.getClaimUuid(), jsonMessage);

            log.info("SERVICE_RESPONSE sent to topic {} for claim: {}", responseTopic, claim.getClaimNumber());

        } catch (Exception e) {
            log.error("Error sending SERVICE_RESPONSE", e);
            throw new RuntimeException("Failed to send SERVICE_RESPONSE", e);
        }
    }

    public void sendStatusUpdate(Claim claim, String previousStatus, String newStatus,
                                 String reason, String operatorId, String operatorName,
                                 String serviceReference, StatusUpdateDto.ResolutionDto resolution) {
        try {
            StatusUpdateDto.AssignedToDto assignedTo = null;
            if (operatorId != null) {
                assignedTo = StatusUpdateDto.AssignedToDto.builder()
                        .operatorId(operatorId)
                        .operatorName(operatorName)
                        .build();
            }

            StatusUpdateDto dto = StatusUpdateDto.builder()
                    .messageId(UUID.randomUUID().toString())
                    .messageType("STATUS_UPDATE")
                    .timestamp(Instant.now().toString())
                    .version("1.0")
                    .claimId(claim.getClaimUuid())
                    .claimNumber(claim.getClaimNumber())
                    .correlationId(claim.getCorrelationUuid())
                    .status(StatusUpdateDto.StatusDto.builder()
                            .previous(previousStatus)
                            .newStatus(newStatus)
                            .reason(reason)
                            .assignedTo(assignedTo)
                            .build())
                    .resolution(resolution)
                    .serviceReference(serviceReference)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send(statusUpdateTopic, claim.getClaimUuid(), jsonMessage);

            log.info("STATUS_UPDATE sent to topic {} for claim: {} (status: {} -> {})",
                    statusUpdateTopic, claim.getClaimNumber(), previousStatus, newStatus);

        } catch (Exception e) {
            log.error("Error sending STATUS_UPDATE", e);
            throw new RuntimeException("Failed to send STATUS_UPDATE", e);
        }
    }
}