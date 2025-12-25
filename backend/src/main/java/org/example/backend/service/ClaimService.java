package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ClaimCreatedDto;
import org.example.backend.DTO.ClaimMessageDto;
import org.example.backend.DTO.StatusUpdateDto;
import org.example.backend.entities.Claim;
import org.example.backend.entities.ClaimAttachment;
import org.example.backend.entities.ClaimMessage;
import org.example.backend.entities.ClaimStatusHistory;
import org.example.backend.repository.ClaimAttachmentRepository;
import org.example.backend.repository.ClaimMessageRepository;
import org.example.backend.repository.ClaimRepository;
import org.example.backend.repository.ClaimStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {


    private final ClaimRepository claimRepository;
    private final ClaimMessageRepository messageRepository;
    private final ClaimStatusHistoryRepository statusHistoryRepository;
    private final ClaimAttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
public Claim processClaimCreated(ClaimCreatedDto dto) {
    log.info("=== STARTING processClaimCreated ===");
    log.info("ClaimId: {}", dto.getClaimId());
    log.info("ClaimNumber: {}", dto.getClaimNumber());

    if (claimRepository.existsByClaimUuid(dto.getClaimId())) {
        log.warn("Claim ALREADY EXISTS: {}", dto.getClaimId());
        return claimRepository.findByClaimUuid(dto.getClaimId()).orElseThrow();
    }

    log.info("Building Claim entity...");
    Claim claim = Claim.builder()
            .messageId(dto.getMessageId())
            .claimUuid(dto.getClaimId())
            .claimNumber(dto.getClaimNumber())
            .correlationUuid(dto.getCorrelationId())
            .userId(dto.getUser().getId())
            .userEmail(dto.getUser().getEmail())
            .userName(dto.getUser().getName())
            .userPhone(dto.getUser().getPhone())
            .serviceType(dto.getClaim().getServiceType())
            .title(dto.getClaim().getTitle())
            .description(dto.getClaim().getDescription())
            .priority(dto.getClaim().getPriority())
            .address(dto.getClaim().getLocation().getAddress())
            .latitude(dto.getClaim().getLocation().getLatitude())
            .longitude(dto.getClaim().getLocation().getLongitude())
            .extraData(serializeToJson(dto.getClaim().getExtraData()))
            .currentStatus("submitted")
            .build();

    log.info("Saving claim to database...");
    claim = claimRepository.save(claim);
    log.info("Claim saved with ID: {}", claim.getId());

    if (dto.getClaim().getAttachments() != null) {
        log.info("Saving {} attachments...", dto.getClaim().getAttachments().size());
        saveAttachments(claim, dto.getClaim().getAttachments(), "INITIAL_CLAIM");
    }

    log.info("Creating status history...");
    createStatusHistory(claim, null, "submitted", "Claim created", null, null);

    log.info("=== CLAIM CREATED SUCCESSFULLY - ID: {} ===", claim.getId());
    return claim;
}
    // a transactional method is a method that is executed within a database transaction
    // a database transaction is a sequence of operations performed as a single logical unit of work 
    // a single logical unit of work must exhibit four properties, known as ACID (Atomicity, Consistency, Isolation, Durability)
    @Transactional
    public ClaimMessage processClaimMessage(ClaimMessageDto dto) {
        log.info("Processing CLAIM_MESSAGE for claimId: {}", dto.getClaimId());

        Claim claim = claimRepository.findByClaimUuid(dto.getClaimId())
                .orElseThrow(() -> new RuntimeException("Claim not found: " + dto.getClaimId()));

        ClaimMessage message = ClaimMessage.builder()
                .claim(claim)
                .messageId(dto.getMessageId())
                .messageType("CLAIM_MESSAGE")
                .messageTimestamp(dto.getTimestamp())
                .senderType("USER")
                .senderId(dto.getUser().getId())
                .senderName(dto.getUser().getName())
                .message(dto.getMessage() != null ? dto.getMessage().getText() : "")
                .attachments(serializeToJson(dto.getAttachments()))
                .build();

        message = messageRepository.save(message);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            saveAttachments(claim, dto.getAttachments(), "USER_MESSAGE");
        }

        log.info("User message saved for claim: {}", claim.getClaimNumber());
        return message;
    }

    @Transactional
    public ClaimMessage sendServiceResponse(Long claimId, String operatorId, String operatorName, 
                                           String responseMessage, String serviceReference,
                                           List<ClaimCreatedDto.AttachmentDto> attachments) {
        log.info("Creating SERVICE_RESPONSE for claimId: {}", claimId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + claimId));

        ClaimMessage message = ClaimMessage.builder()
                .claim(claim)
                .messageId(UUID.randomUUID().toString())
                .messageType("SERVICE_RESPONSE")
                .messageTimestamp(Instant.now().toString())
                .senderType("OPERATOR")
                .senderId(operatorId)
                .senderName(operatorName)
                .message(responseMessage)
                .attachments(serializeToJson(attachments))
                .serviceReference(serviceReference)
                .build();

        message = messageRepository.save(message);

        if (attachments != null && !attachments.isEmpty()) {
            saveAttachments(claim, attachments, "OPERATOR_RESPONSE");
        }

        log.info("Service response saved for claim: {}", claim.getClaimNumber());
        return message;
    }

    @Transactional
    public ClaimStatusHistory updateClaimStatus(Long claimId, String newStatus, String reason,
                                                String operatorId, String operatorName,
                                                String serviceReference,
                                                StatusUpdateDto.ResolutionDto resolution) {
        log.info("Updating status for claimId: {} to {}", claimId, newStatus);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + claimId));

        String previousStatus = claim.getCurrentStatus();

        String assignedToJson = null;
        if (operatorId != null) {
            assignedToJson = String.format("{\"operatorId\":\"%s\",\"operatorName\":\"%s\"}", 
                    operatorId, operatorName);
        }

        ClaimStatusHistory statusHistory = createStatusHistory(
                claim, previousStatus, newStatus, reason, 
                assignedToJson, resolution != null ? serializeToJson(resolution) : null
        );
        statusHistory.setServiceReference(serviceReference);

        claim.setCurrentStatus(newStatus);
        claimRepository.save(claim);

        log.info("Status updated successfully for claim: {}", claim.getClaimNumber());
        return statusHistory;
    }

    private void saveAttachments(Claim claim, List<ClaimCreatedDto.AttachmentDto> attachmentDtos, String source) {
        if (attachmentDtos == null || attachmentDtos.isEmpty()) return;

        for (ClaimCreatedDto.AttachmentDto dto : attachmentDtos) {
            ClaimAttachment attachment = ClaimAttachment.builder()
                    .claim(claim)
                    .url(dto.getUrl())
                    .fileName(dto.getFileName())
                    .fileType(dto.getFileType())
                    .source(source)
                    .build();
            attachmentRepository.save(attachment);
        }
    }

    private ClaimStatusHistory createStatusHistory(Claim claim, String previousStatus, 
                                                   String newStatus, String reason,
                                                   String assignedToJson, String resolutionJson) {
        ClaimStatusHistory history = ClaimStatusHistory.builder()
                .claim(claim)
                .messageId(UUID.randomUUID().toString())
                .messageTimestamp(Instant.now().toString())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .assignedTo(assignedToJson)
                .resolution(resolutionJson)
                .build();

        return statusHistoryRepository.save(history);
    }

    private String serializeToJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error serializing to JSON", e);
            return null;
        }
    }

    public Claim getClaimByUuid(String claimUuid) {
        return claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + claimUuid));
    }

    public List<ClaimMessage> getClaimMessages(Long claimId) {
        return messageRepository.findByClaimIdOrderByCreatedAtAsc(claimId);
    }

    public List<ClaimStatusHistory> getClaimStatusHistory(Long claimId) {
        return statusHistoryRepository.findByClaimIdOrderByCreatedAtAsc(claimId);
    }

    public List<ClaimAttachment> getClaimAttachments(Long claimId) {
        return attachmentRepository.findByClaimId(claimId);
    }
}
