package com.saraasansor.api.repository;

import com.saraasansor.api.model.Elevator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ElevatorRepository extends JpaRepository<Elevator, Long> {
    boolean existsByIdentityNumber(String identityNumber);
    
    java.util.Optional<Elevator> findByIdentityNumber(String identityNumber);
    
    java.util.Optional<Elevator> findByElevatorNumber(String elevatorNumber);
    
    @Query("SELECT e FROM Elevator e WHERE e.expiryDate < :now")
    List<Elevator> findExpiredElevators(LocalDate now);
    
    @Query("SELECT e FROM Elevator e WHERE e.expiryDate >= :now AND e.expiryDate <= :thirtyDaysLater")
    List<Elevator> findExpiringSoonElevators(LocalDate now, LocalDate thirtyDaysLater);
}

