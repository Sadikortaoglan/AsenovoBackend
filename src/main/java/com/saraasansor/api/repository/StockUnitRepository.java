package com.saraasansor.api.repository;

import com.saraasansor.api.model.StockUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockUnitRepository extends JpaRepository<StockUnit, Long> {

    java.util.Optional<StockUnit> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByAbbreviationIgnoreCaseAndActiveTrue(String abbreviation);

    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(String name, Long id);

    boolean existsByAbbreviationIgnoreCaseAndIdNotAndActiveTrue(String abbreviation, Long id);

    @Query("SELECT u FROM StockUnit u WHERE " +
            "(:active IS NULL OR u.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(u.abbreviation, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY u.name ASC, u.id ASC")
    Page<StockUnit> search(@Param("query") String query,
                           @Param("active") Boolean active,
                           Pageable pageable);

    @Query("SELECT u FROM StockUnit u WHERE u.active = true AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(u.abbreviation, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY u.name ASC, u.id ASC")
    List<StockUnit> lookup(@Param("query") String query, Pageable pageable);
}
