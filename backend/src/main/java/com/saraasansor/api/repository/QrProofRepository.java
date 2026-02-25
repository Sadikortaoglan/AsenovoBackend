package com.saraasansor.api.repository;

import com.saraasansor.api.model.QrProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface QrProofRepository extends JpaRepository<QrProof, Long> {
    Optional<QrProof> findByTokenHash(String tokenHash);
    
    @Query("SELECT q FROM QrProof q WHERE q.tokenHash = :tokenHash AND q.expiresAt > :now AND q.usedAt IS NULL")
    Optional<QrProof> findValidToken(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
    
    @Query("SELECT q FROM QrProof q WHERE q.elevator.id = :elevatorId AND q.expiresAt > :now AND q.usedAt IS NULL ORDER BY q.issuedAt DESC")
    java.util.List<QrProof> findActiveTokensForElevator(@Param("elevatorId") Long elevatorId, @Param("now") LocalDateTime now);
}
