package org.example.backend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.backend.enums.Role;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "utilisateur")
@Data
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String motDePasse;

    private String telephone; // utilisé seulement si conducteur

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(unique = true)
    private String clerkId;
}
