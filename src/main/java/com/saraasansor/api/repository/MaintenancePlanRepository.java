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
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.plannedDate >= :from AND p.plannedDate <= :to " +
           "AND p.status IN :statuses " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByPlannedDateBetweenOrderByPlannedDateAsc(
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to,
        @Param("statuses") java.util.List<MaintenancePlan.PlanStatus> statuses);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.plannedDate >= :from AND p.plannedDate <= :to " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByPlannedDateBetweenAllStatuses(
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.elevator.id = :elevatorId " +
           "AND p.plannedDate >= :from AND p.plannedDate <= :to " +
           "AND p.status IN :statuses " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
        @Param("elevatorId") Long elevatorId, 
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to,
        @Param("statuses") java.util.List<MaintenancePlan.PlanStatus> statuses);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.elevator.id = :elevatorId " +
           "AND p.plannedDate >= :from AND p.plannedDate <= :to " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByElevatorIdAndPlannedDateBetweenAllStatuses(
        @Param("elevatorId") Long elevatorId, 
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.assignedTechnician.id = :technicianId " +
           "AND p.plannedDate >= :from AND p.plannedDate <= :to " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByAssignedTechnicianIdAndPlannedDateBetweenOrderByPlannedDateAsc(
        @Param("technicianId") Long technicianId, 
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.plannedDate >= :from AND p.plannedDate <= :to " +
           "AND p.status = :status " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findByPlannedDateBetweenAndStatusOrderByPlannedDateAsc(
        @Param("from") LocalDate from, 
        @Param("to") LocalDate to, 
        @Param("status") MaintenancePlan.PlanStatus status);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE p.id = :id")
    java.util.Optional<MaintenancePlan> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT p FROM MaintenancePlan p " +
           "LEFT JOIN FETCH p.elevator " +
           "LEFT JOIN FETCH p.template " +
           "LEFT JOIN FETCH p.assignedTechnician " +
           "WHERE (:status IS NULL OR p.status = :status) " +
           "AND (:from IS NULL OR p.plannedDate >= :from) " +
           "AND (:to IS NULL OR p.plannedDate <= :to) " +
           "ORDER BY p.plannedDate ASC")
    List<MaintenancePlan> findWithFilters(
        @Param("status") MaintenancePlan.PlanStatus status,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);
}
