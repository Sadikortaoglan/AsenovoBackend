package com.saraasansor.api.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.exception.ValidationException;
import com.saraasansor.api.tenant.dto.TenantCreateAcceptedResponse;
import com.saraasansor.api.tenant.dto.TenantCreateRequest;
import com.saraasansor.api.tenant.dto.TenantExtendLicenseRequest;
import com.saraasansor.api.tenant.dto.TenantProvisioningJobResponse;
import com.saraasansor.api.tenant.dto.TenantResponse;
import com.saraasansor.api.tenant.dto.TenantUpdateRequest;
import com.saraasansor.api.tenant.model.Plan;
import com.saraasansor.api.tenant.model.Subscription;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.repository.PlanRepository;
import com.saraasansor.api.tenant.repository.SubscriptionRepository;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TenantManagementService {

    private static final Set<String> RESERVED_SUBDOMAINS = Set.of("www", "api", "default", "admin");

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantProvisioningJobService tenantProvisioningJobService;
    private final TenantRegistryService tenantRegistryService;
    private final ObjectMapper objectMapper;

    public TenantManagementService(TenantRepository tenantRepository,
                                   PlanRepository planRepository,
                                   SubscriptionRepository subscriptionRepository,
                                   TenantProvisioningJobService tenantProvisioningJobService,
                                   TenantRegistryService tenantRegistryService,
                                   ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantProvisioningJobService = tenantProvisioningJobService;
        this.tenantRegistryService = tenantRegistryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> getTenants(String query, Tenant.TenantStatus status, Pageable pageable) {
        return tenantRepository.search(toSearchPattern(query), status, pageable)
                .map(TenantResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantById(Long id) {
        Tenant tenant = tenantRepository.findByIdWithPlan(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
        return TenantResponse.fromEntity(tenant);
    }

    @Transactional
    public TenantCreateAcceptedResponse createTenant(TenantCreateRequest request) {
        validateLicenseRange(request.getLicenseStartDate(), request.getLicenseEndDate());

        String subdomain = normalizeSubdomain(request.getSubdomain());
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new ValidationException("Subdomain already exists: " + subdomain);
        }

        Plan plan = resolvePlan(request.getPlanType());
        String actor = resolveCurrentUsername();

        Tenant tenant = new Tenant();
        tenant.setCompanyName(normalizeRequired(request.getCompanyName(), "companyName"));
        tenant.setName(tenant.getCompanyName());
        tenant.setSubdomain(subdomain);
        tenant.setTenancyMode(Tenant.TenancyMode.SHARED_SCHEMA);
        tenant.setSchemaName(generatePendingSchemaName(subdomain));
        tenant.setPlanType(request.getPlanType());
        tenant.setStatus(Tenant.TenantStatus.PENDING);
        tenant.setLicenseStartDate(request.getLicenseStartDate());
        tenant.setLicenseEndDate(request.getLicenseEndDate());
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setMaxFacilities(request.getMaxFacilities());
        tenant.setMaxElevators(request.getMaxElevators());
        tenant.setPlan(plan);
        tenant.setRedisNamespace("tenant:" + subdomain);
        tenant.setActive(false);
        tenant.setCreatedBy(actor);
        tenant.setUpdatedBy(actor);

        Tenant savedTenant = tenantRepository.save(tenant);
        synchronizeSubscription(savedTenant);

        TenantProvisioningJob job = tenantProvisioningJobService.enqueueJob(
                savedTenant,
                TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT,
                toCreatePayload(request),
                actor
        );

        TenantCreateAcceptedResponse response = new TenantCreateAcceptedResponse();
        response.setTenant(TenantResponse.fromEntity(savedTenant));
        response.setJob(TenantProvisioningJobResponse.fromEntity(job));
        return response;
    }

    @Transactional
    public TenantResponse updateTenant(Long id, TenantUpdateRequest request) {
        validateLicenseRange(request.getLicenseStartDate(), request.getLicenseEndDate());

        Tenant tenant = tenantRepository.findByIdWithPlan(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));

        tenant.setCompanyName(normalizeRequired(request.getCompanyName(), "companyName"));
        tenant.setName(tenant.getCompanyName());
        tenant.setPlanType(request.getPlanType());
        tenant.setLicenseStartDate(request.getLicenseStartDate());
        tenant.setLicenseEndDate(request.getLicenseEndDate());
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setMaxFacilities(request.getMaxFacilities());
        tenant.setMaxElevators(request.getMaxElevators());
        tenant.setPlan(resolvePlan(request.getPlanType()));
        tenant.setUpdatedBy(resolveCurrentUsername());
        tenant.setUpdatedAt(LocalDateTime.now());

        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
            tenant.setActive(request.getStatus() == Tenant.TenantStatus.ACTIVE);
        }

        Tenant updated = tenantRepository.save(tenant);
        synchronizeSubscription(updated);
        tenantRegistryService.evictCacheForSubdomain(updated.getSubdomain());
        return TenantResponse.fromEntity(updated);
    }

    @Transactional
    public TenantProvisioningJobResponse suspendTenant(Long id) {
        Tenant tenant = getTenantEntity(id);
        TenantProvisioningJob job = tenantProvisioningJobService.enqueueJob(
                tenant,
                TenantProvisioningJob.ProvisioningJobType.SUSPEND_TENANT,
                null,
                resolveCurrentUsername()
        );
        return TenantProvisioningJobResponse.fromEntity(job);
    }

    @Transactional
    public TenantProvisioningJobResponse activateTenant(Long id) {
        Tenant tenant = getTenantEntity(id);
        TenantProvisioningJob job = tenantProvisioningJobService.enqueueJob(
                tenant,
                TenantProvisioningJob.ProvisioningJobType.ACTIVATE_TENANT,
                null,
                resolveCurrentUsername()
        );
        return TenantProvisioningJobResponse.fromEntity(job);
    }

    @Transactional
    public TenantProvisioningJobResponse extendLicense(Long id, TenantExtendLicenseRequest request) {
        Tenant tenant = getTenantEntity(id);
        if (tenant.getLicenseStartDate() != null && request.getLicenseEndDate().isBefore(tenant.getLicenseStartDate())) {
            throw new ValidationException("licenseEndDate cannot be before tenant licenseStartDate");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("licenseEndDate", request.getLicenseEndDate());

        TenantProvisioningJob job = tenantProvisioningJobService.enqueueJob(
                tenant,
                TenantProvisioningJob.ProvisioningJobType.EXTEND_LICENSE,
                serializePayload(payload),
                resolveCurrentUsername()
        );
        return TenantProvisioningJobResponse.fromEntity(job);
    }

    private Tenant getTenantEntity(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + id));
    }

    private Plan resolvePlan(Tenant.PlanType planType) {
        Plan.PlanType targetPlanType = planType == Tenant.PlanType.ENTERPRISE
                ? Plan.PlanType.ENTERPRISE
                : Plan.PlanType.PRO;

        return planRepository.findFirstByPlanTypeAndActiveTrueOrderByIdAsc(targetPlanType)
                .orElseThrow(() -> new ValidationException("No active plan found for " + targetPlanType.name()));
    }

    private void synchronizeSubscription(Tenant tenant) {
        Subscription subscription = subscriptionRepository.findFirstByTenantIdOrderByIdAsc(tenant.getId())
                .orElseGet(Subscription::new);
        subscription.setTenant(tenant);
        subscription.setPlan(tenant.getPlan());
        subscription.setStartsAt(tenant.getLicenseStartDate());
        subscription.setEndsAt(tenant.getLicenseEndDate());
        subscription.setAutoRenew(false);
        subscription.setActive(tenant.getStatus() == Tenant.TenantStatus.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    private String toCreatePayload(TenantCreateRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("initialAdminUsername", normalizeNullable(request.getInitialAdminUsername()));
        payload.put("initialAdminPassword", normalizeNullable(request.getInitialAdminPassword()));
        return serializePayload(payload);
    }

    private String generatePendingSchemaName(String subdomain) {
        return "tenant_pending_" + subdomain.replace('-', '_');
    }

    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Payload serialization failed");
        }
    }

    private void validateLicenseRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("License date range is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("licenseEndDate cannot be before licenseStartDate");
        }
    }

    private String normalizeSubdomain(String value) {
        String normalized = normalizeRequired(value, "subdomain")
                .toLowerCase(Locale.ROOT)
                .replace("_", "-");
        if (RESERVED_SUBDOMAINS.contains(normalized)) {
            throw new ValidationException("Subdomain is reserved: " + normalized);
        }
        if (!normalized.matches("^[a-z0-9-]{2,63}$")) {
            throw new ValidationException("Subdomain format is invalid");
        }
        return normalized;
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toSearchPattern(String query) {
        String normalized = normalizeNullable(query);
        if (normalized == null) {
            return null;
        }
        return "%" + normalized.toLowerCase(Locale.ROOT) + "%";
    }
}
