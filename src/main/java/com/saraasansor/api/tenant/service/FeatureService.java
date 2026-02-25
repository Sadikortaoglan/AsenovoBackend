package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.FeatureNotAvailableException;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.repository.FeatureRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class FeatureService {

    private final FeatureRepository repository;
    private final FeatureService proxyFeatureService;

    public FeatureService(FeatureRepository repository, FeatureService proxyFeatureService) {
        this.repository = repository;
        this.proxyFeatureService = proxyFeatureService;
    }

    @Cacheable(value = "tenantFeatures", key = "#tenantId")
    public Set<String> getEnabledFeatures(String tenantId) {
        return repository.findEnabledFeatures(tenantId);
    }

    public void assertEnabled(String featureCode) throws FeatureNotAvailableException {
        Long tenant = TenantContext.getTenantId();
        if (!proxyFeatureService.getEnabledFeatures(tenant).contains(featureCode)) {
            throw new FeatureNotAvailableException("Feature '" + featureCode + "' is not enabled for tenant '" + tenant + "'.");
        }
    }
}
