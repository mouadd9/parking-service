package org.example.backend.DTO;

import lombok.Data;

@Data
public class ReclamationResponseDTO {
    private String operatorId;
    private String operatorName;
    private Long reclamationId;
    private String responseMessage;
}