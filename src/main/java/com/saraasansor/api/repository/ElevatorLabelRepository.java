package com.saraasansor.api.repository;

import com.saraasansor.api.model.ElevatorLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ElevatorLabelRepository extends JpaRepository<ElevatorLabel, Long> {

    @Query("""
            SELECT l
            FROM ElevatorLabel l
            JOIN l.elevator e
            LEFT JOIN e.facility f
            WHERE LOWER(COALESCE(l.labelName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(l.serialNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(l.contractNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(l.status, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(l.labelType, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(e.elevatorNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(e.identityNumber, '')) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(COALESCE(f.name, e.buildingName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<ElevatorLabel> search(@Param("search") String search, Pageable pageable);
}
