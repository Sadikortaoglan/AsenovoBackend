package com.saraasansor.api.repository;

import com.saraasansor.api.model.Facility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Long> {

    @EntityGraph(attributePaths = {"b2bUnit", "city", "district", "neighborhood", "region"})
    @Query("SELECT f FROM Facility f WHERE " +
            "f.active = true AND " +
            "(:status IS NULL OR f.status = :status) AND " +
            "(:b2bUnitId IS NULL OR f.b2bUnit.id = :b2bUnitId) AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(f.addressText, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(f.companyTitle, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Facility> search(@Param("query") String query,
                          @Param("b2bUnitId") Long b2bUnitId,
                          @Param("status") Facility.FacilityStatus status,
                          Pageable pageable);

    @EntityGraph(attributePaths = {"b2bUnit", "city", "district", "neighborhood", "region"})
    Optional<Facility> findByIdAndActiveTrue(Long id);

    boolean existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrue(Long b2bUnitId, String name);

    boolean existsByB2bUnitIdAndNameIgnoreCaseAndActiveTrueAndIdNot(Long b2bUnitId, String name, Long id);

    long countByB2bUnitIdAndActiveTrue(Long b2bUnitId);

    @EntityGraph(attributePaths = {"b2bUnit"})
    List<Facility> findByB2bUnitIdAndActiveTrue(Long b2bUnitId);

    @EntityGraph(attributePaths = {"b2bUnit"})
    Optional<Facility> findFirstByNameIgnoreCaseAndActiveTrue(String name);

    @EntityGraph(attributePaths = {"b2bUnit"})
    @Query("SELECT f FROM Facility f WHERE f.active = true AND f.b2bUnit.id = :b2bUnitId AND " +
            "(:query IS NULL OR :query = '' OR LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY f.name ASC")
    List<Facility> findLookupByB2bUnitId(@Param("b2bUnitId") Long b2bUnitId, @Param("query") String query, Pageable pageable);
}
