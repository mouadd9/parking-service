package org.example.backend.repository;

import org.example.backend.entities.ClaimMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimMessageRepository extends JpaRepository<ClaimMessage, Long> {
    List<ClaimMessage> findByClaimIdOrderByCreatedAtAsc(Long claimId);
    List<ClaimMessage> findByClaimIdAndSenderTypeOrderByCreatedAtAsc(Long claimId, String senderType);
    long countByClaimId(Long claimId);
}
