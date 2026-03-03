package com.saraasansor.api.repository;

import com.saraasansor.api.model.Region;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findFirstByNeighborhoodIdAndNameIgnoreCase(Long neighborhoodId, String name);

    @Query("SELECT r FROM Region r WHERE r.neighborhood.id = :neighborhoodId AND " +
            "(:query IS NULL OR :query = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY r.name ASC")
    List<Region> search(@Param("neighborhoodId") Long neighborhoodId, @Param("query") String query, Pageable pageable);
}
