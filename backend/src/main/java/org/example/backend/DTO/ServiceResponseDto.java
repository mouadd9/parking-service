package org.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResponseDto {
    private String messageId;
    private String messageType;
    private String timestamp;
    private String version;
    private String claimId;
    private String claimNumber;
    private String correlationId;
    private ResponseDto response;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseDto {
        private FromDto from;
        private String message;
        // private List<ClaimCreatedDto.AttachmentDto> attachments;
        private String serviceReference;
        private List<AttachmentDto> attachments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FromDto {
        private String serviceType;
        private String operatorId;
        private String operatorName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttachmentDto {
        private String url;
        private String fileName;
        private String fileType;
    }
}