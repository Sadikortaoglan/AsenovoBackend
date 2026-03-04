package com.saraasansor.api.repository;

import com.saraasansor.api.model.Neighborhood;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Long> {

    Optional<Neighborhood> findFirstByDistrictIdAndNameIgnoreCase(Long districtId, String name);

    @Query("SELECT n FROM Neighborhood n WHERE n.district.id = :districtId AND " +
            "(:query IS NULL OR :query = '' OR LOWER(n.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY n.name ASC")
    List<Neighborhood> search(@Param("districtId") Long districtId, @Param("query") String query, Pageable pageable);
}
