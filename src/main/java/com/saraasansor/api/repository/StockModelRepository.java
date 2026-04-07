package com.saraasansor.api.repository;

import com.saraasansor.api.model.StockModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockModelRepository extends JpaRepository<StockModel, Long> {

    boolean existsByBrandIdAndNameIgnoreCase(Long brandId, String name);

    boolean existsByBrandIdAndNameIgnoreCaseAndIdNot(Long brandId, String name, Long id);

    @Query("SELECT m FROM StockModel m JOIN m.brand b WHERE " +
            "(:active IS NULL OR m.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(m.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY m.name ASC, m.id ASC")
    Page<StockModel> search(@Param("query") String query,
                            @Param("active") Boolean active,
                            Pageable pageable);

    @Query("SELECT m FROM StockModel m JOIN m.brand b WHERE m.active = true AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(m.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY m.name ASC, m.id ASC")
    List<StockModel> lookup(@Param("query") String query, Pageable pageable);
}
