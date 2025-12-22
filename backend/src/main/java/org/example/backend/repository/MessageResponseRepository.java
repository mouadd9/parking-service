package org.example.backend.repository;

import org.example.backend.entities.MessageResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageResponseRepository extends JpaRepository<MessageResponse, Long> {
}
