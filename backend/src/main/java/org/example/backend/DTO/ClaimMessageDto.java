package org.example.backend.DTO;

// This DTO represents the structure of a claim message (Follow up message not a new claim) received via Kafka
// Also Consumed via claims.rfm topic

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimMessageDto {
    private String messageId;
    private String messageType;
    private String timestamp;
    private String version;
    private String claimId;
    private String claimNumber;
    private String correlationId;
    private ClaimCreatedDto.UserDto user;
    private MessageContentDto message;
    private List<ClaimCreatedDto.AttachmentDto> attachments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageContentDto {
        private String text;
    }
}