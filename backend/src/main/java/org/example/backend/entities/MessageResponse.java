package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reclamationId; // link to table reclamation
    private String operatorId;  // the user who responds (operator or pompier)
    private String operatorName;

    @Column(columnDefinition = "TEXT")
    private String responseMessage;

    private LocalDateTime createdAt;
}