package com.saraasansor.api.repository;

import com.saraasansor.api.model.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    java.util.Optional<Brand> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(String name, Long id);

    @Query("SELECT b FROM Brand b WHERE " +
            "(:active IS NULL OR b.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY b.name ASC, b.id ASC")
    Page<Brand> search(@Param("query") String query,
                       @Param("active") Boolean active,
                       Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE b.active = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY b.name ASC, b.id ASC")
    List<Brand> lookup(@Param("query") String query, Pageable pageable);
}
