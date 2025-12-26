package org.example.backend.repository;

import org.example.backend.entities.Utilisateur;
import org.example.backend.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    //    Optional<User> findByClerkId(String clerkId);
    Optional<Utilisateur> findFirstByClerkId(String clerkId);
    boolean existsByClerkId(String clerkId);
    boolean existsByEmail(String email);


    Optional<Utilisateur> findByEmail(String email);
    long countByRole(Role role);

    Optional<Utilisateur> findByClerkId(String clerkId);
}

