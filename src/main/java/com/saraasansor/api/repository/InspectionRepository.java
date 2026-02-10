package com.saraasansor.api.repository;

import com.saraasansor.api.model.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByElevatorIdOrderByDateDesc(Long elevatorId);
    
    @Query("SELECT i FROM Inspection i JOIN FETCH i.elevator ORDER BY i.date DESC, i.id DESC")
    List<Inspection> findAllWithElevator();
    
    @Query("SELECT i FROM Inspection i JOIN FETCH i.elevator WHERE i.elevator.id = :elevatorId ORDER BY i.date DESC, i.id DESC")
    List<Inspection> findByElevatorIdWithElevator(@Param("elevatorId") Long elevatorId);
    
    @Query("SELECT i FROM Inspection i JOIN FETCH i.elevator WHERE i.id = :id")
    Optional<Inspection> findByIdWithElevator(@Param("id") Long id);
}

