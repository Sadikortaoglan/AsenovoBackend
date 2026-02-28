package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.FeatureNotAvailableException;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.model.Feature;
import com.saraasansor.api.tenant.model.TenantFeatureOverride;
import com.saraasansor.api.tenant.repository.TenantFeatureOverrideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FeatureService {

    private final TenantFeatureOverrideRepository repository;

    @Lazy
    @Autowired
    private FeatureService self;

    public FeatureService(TenantFeatureOverrideRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "tenantFeatures", key = "#tenantId")
    public Set<String> getEnabledFeatures(Long tenantId) {
        List<TenantFeatureOverride> tenantFeatureOverrideList = repository.findTenantFeatureOverridesByTenant_IdAndEnabledTrue(tenantId);
        if (tenantFeatureOverrideList == null || tenantFeatureOverrideList.isEmpty()) {
            return Collections.emptySet();
        }

        List<Feature> features = tenantFeatureOverrideList.stream()
                .map(TenantFeatureOverride::getFeatureDefinition)
                .toList();
        return features.stream().map(Feature::getCode).collect(Collectors.toSet());
    }

    public void assertEnabled(String featureCode) throws FeatureNotAvailableException {
        Long tenant = TenantContext.getTenantId();
        if (!self.getEnabledFeatures(tenant).contains(featureCode)) {
            throw new FeatureNotAvailableException("Feature '" + featureCode + "' is not enabled for tenant '" + tenant + "'.");
        }
    }
}
