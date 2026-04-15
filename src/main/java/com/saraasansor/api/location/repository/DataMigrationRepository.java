package com.saraasansor.api.location.repository;

import com.saraasansor.api.location.model.DataMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataMigrationRepository extends JpaRepository<DataMigration, Long> {

    boolean existsByName(String name);
}
