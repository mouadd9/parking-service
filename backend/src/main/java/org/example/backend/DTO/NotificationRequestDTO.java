package org.example.backend.DTO;

import lombok.Data;

@Data
public class NotificationRequestDTO {
    private String type;
    private String userId;
    private String title;
    private String message;
    private Object data;
}