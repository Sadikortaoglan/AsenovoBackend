package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    Optional<Plan> findFirstByPlanTypeAndActiveTrueOrderByIdAsc(Plan.PlanType planType);
}
