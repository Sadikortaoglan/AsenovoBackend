package com.saraasansor.api.repository;

import com.saraasansor.api.model.B2BUnitGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface B2BUnitGroupRepository extends JpaRepository<B2BUnitGroup, Long> {
    boolean existsByNameIgnoreCase(String name);
}
