package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.TenantFeatureOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantFeatureOverrideRepository extends JpaRepository<TenantFeatureOverride, Long> {
    List<TenantFeatureOverride> findTenantFeatureOverridesByTenant_IdAndEnabledTrue(Long tenantId);
}
