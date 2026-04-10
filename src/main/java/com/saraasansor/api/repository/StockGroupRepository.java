package com.saraasansor.api.repository;

import com.saraasansor.api.model.StockGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockGroupRepository extends JpaRepository<StockGroup, Long> {

    java.util.Optional<StockGroup> findByIdAndActiveTrue(Long id);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndActiveTrue(String name, Long id);

    @Query("SELECT g FROM StockGroup g WHERE " +
            "(:active IS NULL OR g.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(COALESCE(g.name, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY g.name ASC, g.id ASC")
    Page<StockGroup> search(@Param("query") String query,
                            @Param("active") Boolean active,
                            Pageable pageable);
}
