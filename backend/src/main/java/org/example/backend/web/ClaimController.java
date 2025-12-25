package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ClaimCreatedDto;
import org.example.backend.DTO.ServiceResponseDto;
import org.example.backend.DTO.StatusUpdateDto;
import org.example.backend.entities.Claim;
import org.example.backend.entities.ClaimAttachment;
import org.example.backend.entities.ClaimMessage;
import org.example.backend.entities.ClaimStatusHistory;
import org.example.backend.kafka.ClaimKafkaProducer;
import org.example.backend.service.ClaimService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimKafkaProducer kafkaProducer;

    // ============================================
    // Query Endpoints
    // ============================================

    // Will be used in the frontend to fetch claim in the operator dashboard

    /**
     * Get claim by UUID
     * GET /api/claims/{claimUuid}
     */
    @GetMapping("/{claimUuid}")
    public ResponseEntity<Claim> getClaimByUuid(@PathVariable String claimUuid) {
        Claim claim = claimService.getClaimByUuid(claimUuid);
        return ResponseEntity.ok(claim);
    }

    /**
     * Get all messages for a claim
     * GET /api/claims/{claimId}/messages
     */
    @GetMapping("/{claimId}/messages")
    public ResponseEntity<List<ClaimMessage>> getClaimMessages(@PathVariable Long claimId) {
        List<ClaimMessage> messages = claimService.getClaimMessages(claimId);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get status history for a claim
     * GET /api/claims/{claimId}/status-history
     */
    @GetMapping("/{claimId}/status-history")
    public ResponseEntity<List<ClaimStatusHistory>> getStatusHistory(@PathVariable Long claimId) {
        List<ClaimStatusHistory> history = claimService.getClaimStatusHistory(claimId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get attachments for a claim
     * GET /api/claims/{claimId}/attachments
     */
    @GetMapping("/{claimId}/attachments")
    public ResponseEntity<List<ClaimAttachment>> getAttachments(@PathVariable Long claimId) {
        List<ClaimAttachment> attachments = claimService.getClaimAttachments(claimId);
        return ResponseEntity.ok(attachments);
    }

    // ============================================
    // Action Endpoints (Operator Actions)
    // ============================================

    /**
     * Operator sends a response to the user
     * POST /api/claims/{claimId}/respond
     */
    @PostMapping("/{claimId}/respond")
    public ResponseEntity<ClaimMessage> sendResponse(
            @PathVariable Long claimId,
            @RequestBody ResponseRequest request) {

        log.info("Operator {} responding to claim {}", request.getOperatorId(), claimId);

        // Save response in database
        ClaimMessage message = claimService.sendServiceResponse(
                claimId,
                request.getOperatorId(),
                request.getOperatorName(),
                request.getMessage(),
                request.getServiceReference(),
                request.getAttachments()
        );

        // Send to Kafka
        Claim claim = claimService.getClaimByUuid(message.getClaim().getClaimUuid());

        // Convert attachments to the correct type
        List<ServiceResponseDto.AttachmentDto> kafkaAttachments = null;
        if (request.getAttachments() != null) {
            kafkaAttachments = request.getAttachments().stream()
                    .map(att -> ServiceResponseDto.AttachmentDto.builder()
                            .url(att.getUrl())
                            .fileName(att.getFileName())
                            .fileType(att.getFileType())
                            .build())
                    .toList();
        }

        kafkaProducer.sendServiceResponse(
                claim,
                request.getOperatorId(),
                request.getOperatorName(),
                request.getMessage(),
                request.getServiceReference(),
                kafkaAttachments
        );

        return ResponseEntity.ok(message);
    }

    /**
     * Operator updates claim status
     * POST /api/claims/{claimId}/status
     */
    @PostMapping("/{claimId}/status")
    public ResponseEntity<ClaimStatusHistory> updateStatus(
            @PathVariable Long claimId,
            @RequestBody StatusUpdateRequest request) {

        log.info("Updating claim {} status to {}", claimId, request.getNewStatus());

        // Save status in database
        ClaimStatusHistory statusHistory = claimService.updateClaimStatus(
                claimId,
                request.getNewStatus(),
                request.getReason(),
                request.getOperatorId(),
                request.getOperatorName(),
                request.getServiceReference(),
                request.getResolution()
        );

        // Send to Kafka
        Claim claim = claimService.getClaimByUuid(statusHistory.getClaim().getClaimUuid());
        kafkaProducer.sendStatusUpdate(
                claim,
                statusHistory.getPreviousStatus(),
                request.getNewStatus(),
                request.getReason(),
                request.getOperatorId(),
                request.getOperatorName(),
                request.getServiceReference(),
                request.getResolution()
        );

        return ResponseEntity.ok(statusHistory);
    }

    @PostMapping("/test")
    public ResponseEntity<Claim> testCreateClaim(@RequestBody ClaimCreatedDto dto) {
        log.info("TEST: Creating claim manually");
        Claim claim = claimService.processClaimCreated(dto);
        return ResponseEntity.ok(claim);
    }
    // ============================================
    // Request DTOs
    // ============================================

    @lombok.Data
    public static class ResponseRequest {
        private String operatorId;
        private String operatorName;
        private String message;
        private String serviceReference;
        private List<ClaimCreatedDto.AttachmentDto> attachments;
    }

    @lombok.Data
    public static class StatusUpdateRequest {
        private String newStatus;
        private String reason;
        private String operatorId;
        private String operatorName;
        private String serviceReference;
        private StatusUpdateDto.ResolutionDto resolution;
    }
}