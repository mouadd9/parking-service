package org.example.backend.repository;

import org.example.backend.entities.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimUuid(String claimUuid);
    Optional<Claim> findByClaimNumber(String claimNumber);
    List<Claim> findByUserId(String userId);
    List<Claim> findByCurrentStatus(String currentStatus);
    List<Claim> findByServiceType(String serviceType);
    boolean existsByClaimUuid(String claimUuid);
}
