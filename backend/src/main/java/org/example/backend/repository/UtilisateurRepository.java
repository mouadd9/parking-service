package org.example.backend.repository;

import org.example.backend.entities.Utilisateur;
import org.example.backend.enums.Role;
import org.example.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findFirstByClerkId(String clerkId);
    Optional<Utilisateur> findByClerkId(String clerkId);

    boolean existsByClerkId(String clerkId);
    boolean existsByEmail(String email);

    long countByRole(Role role);
}

