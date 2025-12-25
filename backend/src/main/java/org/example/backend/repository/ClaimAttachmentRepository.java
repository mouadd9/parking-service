package org.example.backend.repository;

import org.example.backend.entities.ClaimAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimAttachmentRepository extends JpaRepository<ClaimAttachment, Long> {
    List<ClaimAttachment> findByClaimId(Long claimId);
    List<ClaimAttachment> findByClaimIdAndSource(Long claimId, String source);
    long countByClaimId(Long claimId);
}
