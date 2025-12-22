package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(columnDefinition = "TEXT")
    private String attachmentsJson; // store full JSON array string

    @Column(columnDefinition = "TEXT")
    private String extraDataJson; // store full JSON object string

    private LocalDateTime receivedAt;
}