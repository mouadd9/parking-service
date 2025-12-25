package org.example.backend.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusUpdateDto {
    private String messageId;
    private String messageType;
    private String timestamp;
    private String version;
    private String claimId;
    private String claimNumber;
    private String correlationId;
    private StatusDto status;
    private ResolutionDto resolution;
    private String serviceReference;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusDto {
        private String previous;
        @JsonProperty("new")
        private String newStatus;
        private String reason;
        private AssignedToDto assignedTo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AssignedToDto {
        private String operatorId;
        private String operatorName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResolutionDto {
        private String summary;
        private List<String> actionsTaken;
        private String closingMessage;
    }
}