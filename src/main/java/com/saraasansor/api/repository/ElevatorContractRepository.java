package com.saraasansor.api.repository;

import com.saraasansor.api.model.ElevatorContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElevatorContractRepository extends JpaRepository<ElevatorContract, Long> {

    @EntityGraph(attributePaths = {"elevator", "elevator.facility"})
    @Query("""
            SELECT c
            FROM ElevatorContract c
            LEFT JOIN c.elevator e
            LEFT JOIN e.facility f
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(COALESCE(e.elevatorNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(e.identityNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(e.buildingName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.status, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:elevatorId IS NULL OR e.id = :elevatorId)
            """)
    Page<ElevatorContract> search(@Param("search") String search,
                                  @Param("elevatorId") Long elevatorId,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"elevator", "elevator.facility"})
    @Query("""
            SELECT c
            FROM ElevatorContract c
            JOIN c.elevator e
            JOIN e.facility f
            WHERE f.b2bUnit.id = :b2bUnitId
              AND (:search IS NULL OR :search = '' OR
                   LOWER(COALESCE(e.elevatorNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(e.identityNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(e.buildingName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(c.status, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:elevatorId IS NULL OR e.id = :elevatorId)
            """)
    Page<ElevatorContract> searchByB2bUnit(@Param("b2bUnitId") Long b2bUnitId,
                                           @Param("search") String search,
                                           @Param("elevatorId") Long elevatorId,
                                           Pageable pageable);

    @EntityGraph(attributePaths = {"elevator", "elevator.facility"})
    @Query("SELECT c FROM ElevatorContract c WHERE c.id = :id")
    Optional<ElevatorContract> findDetailById(@Param("id") Long id);
}
