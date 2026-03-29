package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.data.TenantResolutionResult;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        TenantResolutionResult resolution = resolveBySubdomain(subdomain);
        if (resolution.getStatus() == TenantResolutionResult.ResolutionStatus.RESOLVED) {
            return Optional.ofNullable(resolution.getTenantDescriptor());
        }
        return Optional.empty();
    }

    @Transactional
    public TenantResolutionResult resolveBySubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) {
            return TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.TENANT_NOT_FOUND,
                    "Tenant subdomain is missing"
            );
        }

        TenantDescriptor cached = cacheBySubdomain.get(subdomain);
        if (cached != null) {
            TenantResolutionResult cachedResolution = validateRuntimeState(cached);
            if (cachedResolution.getStatus() == TenantResolutionResult.ResolutionStatus.EXPIRED) {
                markTenantExpired(subdomain);
            }
            return cachedResolution;
        }

        Optional<Tenant> tenantOpt = tenantRepository.findBySubdomainWithPlan(subdomain);
        if (tenantOpt.isEmpty()) {
            return TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.TENANT_NOT_FOUND,
                    "Unknown tenant for subdomain: " + subdomain
            );
        }

        Tenant tenant = tenantOpt.get();

        if (tenant.getStatus() == Tenant.TenantStatus.ACTIVE && isLicenseExpired(tenant.getLicenseEndDate())) {
            tenant.setStatus(Tenant.TenantStatus.EXPIRED);
            tenant.setActive(false);
            tenant.setUpdatedAt(LocalDateTime.now());
            tenantRepository.save(tenant);
            evictCacheForSubdomain(subdomain);
        }

        TenantDescriptor descriptor = toDescriptor(tenant);
        TenantResolutionResult resolution = validateRuntimeState(descriptor);
        if (resolution.getStatus() == TenantResolutionResult.ResolutionStatus.RESOLVED) {
            cacheBySubdomain.put(subdomain, descriptor);
        }
        return resolution;
    }

    public void evictCacheForSubdomain(String subdomain) {
        if (subdomain != null) {
            cacheBySubdomain.remove(subdomain);
        }
    }

    private TenantDescriptor toDescriptor(Tenant tenant) {
        String planTypeValue = tenant.getPlanType() != null
                ? tenant.getPlanType().name()
                : (tenant.getPlan() != null ? tenant.getPlan().getPlanType().name() : null);

        return new TenantDescriptor(
                tenant.getId(),
                tenant.getCompanyName(),
                tenant.getSubdomain(),
                tenant.getTenancyMode(),
                tenant.getSchemaName(),
                tenant.getDbHost(),
                tenant.getDbName(),
                tenant.getDbUsername(),
                tenant.getDbPassword(),
                tenant.getRedisNamespace(),
                planTypeValue,
                tenant.getStatus(),
                tenant.getLicenseStartDate(),
                tenant.getLicenseEndDate()
        );
    }

    private TenantResolutionResult validateRuntimeState(TenantDescriptor descriptor) {
        if (descriptor == null) {
            return TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.TENANT_NOT_FOUND,
                    "Tenant not found"
            );
        }

        if (descriptor.getStatus() == null) {
            return TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.INACTIVE,
                    "Tenant status is invalid"
            );
        }

        return switch (descriptor.getStatus()) {
            case ACTIVE -> validateLicense(descriptor);
            case SUSPENDED -> TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.SUSPENDED,
                    "Tenant is suspended"
            );
            case EXPIRED -> TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.EXPIRED,
                    "Tenant license is expired"
            );
            case PENDING -> TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.PENDING,
                    "Tenant provisioning is pending"
            );
            case PROVISIONING_FAILED -> TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.PROVISIONING_FAILED,
                    "Tenant provisioning failed"
            );
            case DELETED -> TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.DELETED,
                    "Tenant is deleted"
            );
        };
    }

    private TenantResolutionResult validateLicense(TenantDescriptor descriptor) {
        LocalDate now = LocalDate.now();
        LocalDate start = descriptor.getLicenseStartDate();
        LocalDate end = descriptor.getLicenseEndDate();

        if (start != null && now.isBefore(start)) {
            return TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.PENDING,
                    "Tenant license is not started yet"
            );
        }

        if (end != null && now.isAfter(end)) {
            return TenantResolutionResult.blocked(
                    TenantResolutionResult.ResolutionStatus.EXPIRED,
                    "Tenant license is expired"
            );
        }

        return TenantResolutionResult.resolved(descriptor);
    }

    private boolean isLicenseExpired(LocalDate licenseEndDate) {
        return licenseEndDate != null && LocalDate.now().isAfter(licenseEndDate);
    }

    private void markTenantExpired(String subdomain) {
        tenantRepository.findBySubdomainWithPlan(subdomain).ifPresent(tenant -> {
            if (tenant.getStatus() == Tenant.TenantStatus.ACTIVE && isLicenseExpired(tenant.getLicenseEndDate())) {
                tenant.setStatus(Tenant.TenantStatus.EXPIRED);
                tenant.setActive(false);
                tenant.setUpdatedAt(LocalDateTime.now());
                tenantRepository.save(tenant);
                evictCacheForSubdomain(subdomain);
            }
        });
    }
}
