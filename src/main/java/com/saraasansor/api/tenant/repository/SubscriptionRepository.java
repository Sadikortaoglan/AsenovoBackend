package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("""
        SELECT s FROM Subscription s
        JOIN FETCH s.plan
        WHERE s.tenant.id = :tenantId
        AND s.active = true
        AND s.startsAt <= CURRENT_TIMESTAMP
        AND (s.endsAt IS NULL OR s.endsAt >= CURRENT_TIMESTAMP)
    """)
    Optional<Subscription> findActiveSubscription(Long tenantId);

    Optional<Subscription> findByTenantIdAndActiveTrue(Long tenantId);
}
