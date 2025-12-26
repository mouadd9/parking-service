package org.example.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclamationDTO {
    private Long id;
    private String userId;
    private String userEmail;
    private String userName;
    private String userPhone;
    private String serviceType;
    private String title;
    private String description;
    private String priority;
    private String address;
    private Double latitude;
    private Double longitude;
    private String attachmentsJson;
    private String extraDataJson;
    private LocalDateTime receivedAt;
}
