package org.example.backend.repository;

import org.example.backend.entities.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByUserId(String userId);
    List<Reclamation> findAllByOrderByReceivedAtDesc();
}
