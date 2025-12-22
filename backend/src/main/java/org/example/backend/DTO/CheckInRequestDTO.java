package org.example.backend.DTO;

import lombok.Data;

@Data
public class CheckInRequestDTO {
    // QUESTION: Later, will this be extracted from the JWT Token?
    // For now, we send it in the JSON body to test the logic.
    private String userId;
}
