package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
}
