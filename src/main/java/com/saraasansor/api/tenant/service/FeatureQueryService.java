package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.model.Feature;
import com.saraasansor.api.tenant.model.TenantFeatureOverride;
import com.saraasansor.api.tenant.repository.TenantFeatureOverrideRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeatureQueryService {

    private final TenantFeatureOverrideRepository repository;

    public FeatureQueryService(TenantFeatureOverrideRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "tenantFeatures", key = "#tenantId")
    public Set<String> getEnabledFeatures(Long tenantId) {
        List<TenantFeatureOverride> tenantFeatureOverrideList =
                repository.findTenantFeatureOverridesByTenant_IdAndEnabledTrue(tenantId);
        if (tenantFeatureOverrideList == null || tenantFeatureOverrideList.isEmpty()) {
            return Collections.emptySet();
        }

        return tenantFeatureOverrideList.stream()
                .map(TenantFeatureOverride::getFeatureDefinition)
                .map(Feature::getCode)
                .collect(Collectors.toSet());
    }
}
