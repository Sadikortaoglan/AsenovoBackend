package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceStepResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceStepResultRepository extends JpaRepository<MaintenanceStepResult, Long> {
    List<MaintenanceStepResult> findBySessionIdOrderByItemIdAsc(Long sessionId);
    Optional<MaintenanceStepResult> findBySessionIdAndItemId(Long sessionId, Long itemId);
}
