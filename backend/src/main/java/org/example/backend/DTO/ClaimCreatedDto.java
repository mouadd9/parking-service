package org.example.backend.DTO;


// This DTO represents the structure of a claim creation message received via Kafka
// Consumed via claims.rfm topic

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCreatedDto {
    private String messageId;
    private String messageType;
    private String timestamp;
    private String version;
    private String claimId;
    private String claimNumber;
    private String correlationId;
    private UserDto user;
    private ClaimDetailsDto claim;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClaimDetailsDto {
        private String serviceType;
        private String title;
        private String description;
        private String priority;
        private LocationDto location;
        private List<AttachmentDto> attachments;
        private Object extraData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private String url;
        private String fileName;
        private String fileType;
    }
}