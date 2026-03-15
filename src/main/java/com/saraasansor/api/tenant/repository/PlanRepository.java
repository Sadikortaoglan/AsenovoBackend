package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findByCodeAndActiveTrue(String code);
    Optional<Plan> findFirstByPlanTypeAndActiveTrue(Plan.PlanType planType);
}
