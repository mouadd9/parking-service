package org.example.backend.repository;

import org.example.backend.entities.ClaimStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, Long> {
    List<ClaimStatusHistory> findByClaimIdOrderByCreatedAtAsc(Long claimId);
    Optional<ClaimStatusHistory> findFirstByClaimIdOrderByCreatedAtDesc(Long claimId);
    List<ClaimStatusHistory> findByNewStatus(String newStatus);
}
