package com.saraasansor.api.repository;

import com.saraasansor.api.model.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    Optional<Part> findByIdAndActiveTrue(Long id);

    boolean existsByCodeIgnoreCaseAndActiveTrue(String code);

    boolean existsByBarcodeIgnoreCaseAndActiveTrue(String barcode);

    boolean existsByCodeIgnoreCaseAndIdNotAndActiveTrue(String code, Long id);

    boolean existsByBarcodeIgnoreCaseAndIdNotAndActiveTrue(String barcode, Long id);

    @Query("SELECT p FROM Part p WHERE " +
            "(:active IS NULL OR p.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(p.code, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY p.name ASC, p.id ASC")
    Page<Part> search(@Param("query") String query,
                      @Param("active") Boolean active,
                      Pageable pageable);

    @Query("SELECT p FROM Part p WHERE p.active = true AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(p.code, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY p.name ASC, p.id ASC")
    List<Part> lookup(@Param("query") String query, Pageable pageable);
}
