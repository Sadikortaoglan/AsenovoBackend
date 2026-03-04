package com.saraasansor.api.repository;

import com.saraasansor.api.model.Fault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaultRepository extends JpaRepository<Fault, Long> {
    List<Fault> findByElevatorIdOrderByCreatedAtDesc(Long elevatorId);
    List<Fault> findByElevatorIdInOrderByCreatedAtDesc(List<Long> elevatorIds);
    List<Fault> findByStatusOrderByCreatedAtDesc(Fault.Status status);
    List<Fault> findByElevatorIdAndStatusOrderByCreatedAtDesc(Long elevatorId, Fault.Status status);
    
    @Query("SELECT f FROM Fault f JOIN FETCH f.elevator ORDER BY f.createdAt DESC")
    List<Fault> findAllWithElevator();
    
    @Query("SELECT f FROM Fault f JOIN FETCH f.elevator WHERE f.status = :status ORDER BY f.createdAt DESC")
    List<Fault> findByStatusWithElevator(@Param("status") Fault.Status status);
    
    @Query("SELECT f FROM Fault f JOIN FETCH f.elevator WHERE f.id = :id")
    Optional<Fault> findByIdWithElevator(@Param("id") Long id);
}
