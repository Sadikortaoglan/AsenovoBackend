package com.saraasansor.api.repository;

import com.saraasansor.api.model.B2BUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface B2BUnitRepository extends JpaRepository<B2BUnit, Long> {

    @EntityGraph(attributePaths = {"group"})
    @Query("SELECT u FROM B2BUnit u LEFT JOIN u.group g WHERE " +
            "u.active = true AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(u.taxNumber, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(g.name, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<B2BUnit> search(@Param("query") String query, Pageable pageable);

    @EntityGraph(attributePaths = {"group"})
    Optional<B2BUnit> findByIdAndActiveTrue(Long id);

    boolean existsByPortalUsernameAndActiveTrue(String portalUsername);

    boolean existsByPortalUsernameAndActiveTrueAndIdNot(String portalUsername, Long id);
}
