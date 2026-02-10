package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenancePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {
    List<MaintenancePlan> findByPlannedDateBetweenOrderByPlannedDateAsc(LocalDate from, LocalDate to);
    
    List<MaintenancePlan> findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
        Long elevatorId, LocalDate from, LocalDate to);
    
    List<MaintenancePlan> findByAssignedTechnicianIdAndPlannedDateBetweenOrderByPlannedDateAsc(
        Long technicianId, LocalDate from, LocalDate to);
    
    @Query("SELECT p FROM MaintenancePlan p WHERE p.plannedDate >= :from AND p.plannedDate <= :to AND p.status = :status ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByPlannedDateBetweenAndStatusOrderByPlannedDateAsc(
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to, 
        @Param("status") MaintenancePlan.PlanStatus status);
}
