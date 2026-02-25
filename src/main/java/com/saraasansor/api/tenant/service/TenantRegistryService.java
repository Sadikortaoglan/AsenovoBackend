package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TenantRegistryService {

    private final TenantRepository tenantRepository;

    private final Map<String, TenantDescriptor> cacheBySubdomain = new ConcurrentHashMap<>();

    public TenantRegistryService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Optional<TenantDescriptor> findActiveBySubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) {
            return Optional.empty();
        }

        TenantDescriptor cached = cacheBySubdomain.get(subdomain);
        if (cached != null) {
            return Optional.of(cached);
        }

        return tenantRepository.findBySubdomainAndActiveIsTrue(subdomain)
                .map(this::toDescriptor)
                .map(descriptor -> {
                    cacheBySubdomain.put(subdomain, descriptor);
                    return descriptor;
                });
    }

    public void evictCacheForSubdomain(String subdomain) {
        if (subdomain != null) {
            cacheBySubdomain.remove(subdomain);
        }
    }

    private TenantDescriptor toDescriptor(Tenant tenant) {
        return new TenantDescriptor(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getTenancyMode(),
                tenant.getSchemaName(),
                tenant.getDbHost(),
                tenant.getDbName(),
                tenant.getDbUsername(),
                tenant.getDbPassword(),
                tenant.getRedisNamespace(),
                tenant.getPlan().getPlanType().name()
        );
    }
}

