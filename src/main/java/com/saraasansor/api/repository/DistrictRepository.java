package com.saraasansor.api.repository;

import com.saraasansor.api.model.District;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

    Optional<District> findFirstByCityIdAndNameIgnoreCase(Long cityId, String name);

    long countByCityId(Long cityId);

    @Query("SELECT d FROM District d JOIN FETCH d.city")
    List<District> findAllWithCity();

    @Query("SELECT d FROM District d WHERE d.city.id = :cityId AND " +
            "(:query IS NULL OR :query = '' OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY d.name ASC")
    List<District> search(@Param("cityId") Long cityId, @Param("query") String query, Pageable pageable);

    @Query("SELECT d FROM District d JOIN FETCH d.city WHERE d.city.id IN :cityIds AND " +
            "(:query IS NULL OR :query = '' OR LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY d.name ASC")
    List<District> searchByCityIds(@Param("cityIds") Collection<Long> cityIds, @Param("query") String query, Pageable pageable);
}
