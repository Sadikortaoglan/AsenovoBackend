package com.saraasansor.api.repository;

import com.saraasansor.api.model.Warehouse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByIdAndActiveTrue(Long id);

    @Query("SELECT w FROM Warehouse w WHERE w.active = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY w.name ASC")
    List<Warehouse> findLookup(@Param("query") String query, Pageable pageable);
}
