package com.saraasansor.api.repository;

import com.saraasansor.api.model.City;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findFirstByNameIgnoreCase(String name);

    @Query("SELECT c FROM City c WHERE (:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY c.name ASC")
    List<City> search(@Param("query") String query, Pageable pageable);
}
