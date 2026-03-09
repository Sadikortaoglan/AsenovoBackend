package com.saraasansor.api.repository;

import com.saraasansor.api.model.Elevator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Elevator> findByBuildingNameIgnoreCase(String buildingName);

    @Query("SELECT e FROM Elevator e WHERE LOWER(e.buildingName) IN :buildingNames")
    List<Elevator> findByBuildingNames(@Param("buildingNames") List<String> buildingNames);

    @Query("SELECT e FROM Elevator e WHERE " +
            "LOWER(e.buildingName) IN :buildingNames AND " +
            "(:search IS NULL OR :search = '' OR " +
            "LOWER(COALESCE(e.elevatorNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(e.buildingName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(e.identityNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(e.machineBrand, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Elevator> searchByBuildingNames(@Param("buildingNames") List<String> buildingNames,
                                         @Param("search") String search,
                                         Pageable pageable);
}
