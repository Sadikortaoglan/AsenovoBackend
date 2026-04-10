package com.saraasansor.api.tenant.service.impl;

import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.exception.ValidationException;
import com.saraasansor.api.security.AuthenticatedUserContext;
import com.saraasansor.api.security.AuthenticatedUserContextService;
import com.saraasansor.api.security.UserAuthorizationPolicyService;
import com.saraasansor.api.service.FileStorageService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.dto.TenantBrandingResponseDTO;
import com.saraasansor.api.tenant.dto.TenantBrandingUpdateRequestDTO;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import com.saraasansor.api.tenant.service.TenantBrandingService;
import com.saraasansor.api.tenant.validation.BrandingValidator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TenantBrandingServiceImpl implements TenantBrandingService {

    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/svg+xml"
    );

    private final TenantRepository tenantRepository;
    private final BrandingValidator brandingValidator;
    private final FileStorageService fileStorageService;
    private final AuthenticatedUserContextService authenticatedUserContextService;
    private final UserAuthorizationPolicyService userAuthorizationPolicyService;

    public TenantBrandingServiceImpl(TenantRepository tenantRepository,
                                     BrandingValidator brandingValidator,
                                     FileStorageService fileStorageService,
                                     AuthenticatedUserContextService authenticatedUserContextService,
                                     UserAuthorizationPolicyService userAuthorizationPolicyService) {
        this.tenantRepository = tenantRepository;
        this.brandingValidator = brandingValidator;
        this.fileStorageService = fileStorageService;
        this.authenticatedUserContextService = authenticatedUserContextService;
        this.userAuthorizationPolicyService = userAuthorizationPolicyService;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = RuntimeException.class)
    public TenantBrandingResponseDTO getCurrentTenantBranding() {
        Tenant tenant = findTenant();
        return toResponse(tenant);
    }

    @Override
    @Transactional
    public TenantBrandingResponseDTO updateBranding(TenantBrandingUpdateRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new ValidationException("Request body is required");
        }

        requireBrandingEditPermission();
        String companyName = normalizeOptional(requestDTO.getCompanyName(), "companyName");
        String primaryColor = normalizeOptional(requestDTO.getPrimaryColor(), "primaryColor");
        String secondaryColor = normalizeOptional(requestDTO.getSecondaryColor(), "secondaryColor");

        if (companyName == null && primaryColor == null && secondaryColor == null) {
            throw new ValidationException("At least one field must be provided for update");
        }

        brandingValidator.validateColorsForUpdate(primaryColor, secondaryColor);

        Tenant tenant = findTenant();
        if (companyName != null) {
            tenant.setCompanyName(companyName);
            tenant.setName(companyName);
        }
        if (primaryColor != null) {
            tenant.setPrimaryColor(primaryColor);
        }
        if (secondaryColor != null) {
            tenant.setSecondaryColor(secondaryColor);
        }
        tenant.setBrandingUpdatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        return toResponse(tenantRepository.save(tenant));
    }

    @Override
    @Transactional
    public TenantBrandingResponseDTO updateLogo(MultipartFile file) {
        requireBrandingEditPermission();
        Long tenantId = resolveAuthenticatedTenantId();
        validateLogoFile(file);

        Tenant tenant = findTenant();

        try {
            String fileName = UUID.randomUUID() + resolveExtension(file);
            String directory = "tenants/" + tenantId + "/branding";
            String storageKey = fileStorageService.saveFileToDirectory(file, directory, fileName);
            String logoUrl = fileStorageService.getFileUrl(storageKey);

            tenant.setLogoUrl(logoUrl);
            tenant.setBrandingUpdatedAt(LocalDateTime.now());
            tenant.setUpdatedAt(LocalDateTime.now());
            return toResponse(tenantRepository.save(tenant));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to upload tenant logo", ex);
        }
    }

    private Long resolveAuthenticatedTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException("User not authenticated");
        }

        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("Tenant context is missing");
        }
        return tenantId;
    }

    private Tenant findTenant() {
        TenantDescriptor currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null) {
            throw new RuntimeException("Tenant context is missing");
        }

        if (currentTenant.getSubdomain() != null && !currentTenant.getSubdomain().isBlank()) {
            return tenantRepository.findBySubdomain(currentTenant.getSubdomain())
                    .orElseGet(() -> findTenantByIdOrFallback(currentTenant.getId()));
        }

        return findTenantByIdOrFallback(currentTenant.getId());
    }

    private Tenant findTenantByIdOrFallback(Long tenantId) {
        if (tenantId != null) {
            return tenantRepository.findById(tenantId)
                    .orElseGet(() -> tenantRepository.findFirstByOrderByIdAsc()
                            .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId)));
        }

        return tenantRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NotFoundException("Tenant not found"));
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Logo file is required");
        }

        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new ValidationException("Logo file size must be less than or equal to 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ValidationException("Logo file type must be PNG, SVG, or JPG");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/svg+xml".equals(contentType)) {
            return ".svg";
        }
        return ".jpg";
    }

    private String normalizeOptional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
        return normalized;
    }

    private void requireBrandingEditPermission() {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        userAuthorizationPolicyService.requireTenantScopedUserManager(actor);
    }

    private TenantBrandingResponseDTO toResponse(Tenant tenant) {
        TenantBrandingResponseDTO response = new TenantBrandingResponseDTO();
        String companyName = tenant.getCompanyName();
        response.setId(tenant.getId());
        response.setCompanyName(companyName);
        response.setName(companyName);
        response.setLogoUrl(tenant.getLogoUrl());
        response.setPrimaryColor(tenant.getPrimaryColor());
        response.setSecondaryColor(tenant.getSecondaryColor());
        response.setBrandingUpdatedAt(tenant.getBrandingUpdatedAt());
        return response;
    }
}
