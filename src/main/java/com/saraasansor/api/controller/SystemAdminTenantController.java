package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.tenant.dto.TenantCreateAcceptedResponse;
import com.saraasansor.api.tenant.dto.TenantCreateRequest;
import com.saraasansor.api.tenant.dto.TenantExtendLicenseRequest;
import com.saraasansor.api.tenant.dto.TenantProvisioningJobResponse;
import com.saraasansor.api.tenant.dto.TenantResponse;
import com.saraasansor.api.tenant.dto.TenantUpdateRequest;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.service.TenantManagementService;
import com.saraasansor.api.tenant.service.TenantProvisioningJobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system-admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class SystemAdminTenantController {

    private final TenantManagementService tenantManagementService;
    private final TenantProvisioningJobService tenantProvisioningJobService;

    public SystemAdminTenantController(TenantManagementService tenantManagementService,
                                       TenantProvisioningJobService tenantProvisioningJobService) {
        this.tenantManagementService = tenantManagementService;
        this.tenantProvisioningJobService = tenantProvisioningJobService;
    }

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> getTenants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Tenant.TenantStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        Page<TenantResponse> response = tenantManagementService.getTenants(
                query,
                status,
                PageRequest.of(page, size, parseSort(sort))
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tenants/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(tenantManagementService.getTenantById(id)));
    }

    @PostMapping("/tenants")
    public ResponseEntity<ApiResponse<TenantCreateAcceptedResponse>> createTenant(
            @Valid @RequestBody TenantCreateRequest request) {
        TenantCreateAcceptedResponse response = tenantManagementService.createTenant(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Tenant provisioning requested", response));
    }

    @PutMapping("/tenants/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(@PathVariable Long id,
                                                                    @Valid @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant updated", tenantManagementService.updateTenant(id, request)));
    }

    @PostMapping("/tenants/{id}/suspend")
    public ResponseEntity<ApiResponse<TenantProvisioningJobResponse>> suspendTenant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant suspend job queued", tenantManagementService.suspendTenant(id)));
    }

    @PostMapping("/tenants/{id}/activate")
    public ResponseEntity<ApiResponse<TenantProvisioningJobResponse>> activateTenant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant activate job queued", tenantManagementService.activateTenant(id)));
    }

    @PostMapping("/tenants/{id}/extend-license")
    public ResponseEntity<ApiResponse<TenantProvisioningJobResponse>> extendLicense(
            @PathVariable Long id,
            @Valid @RequestBody TenantExtendLicenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant license extension job queued",
                tenantManagementService.extendLicense(id, request)));
    }

    @GetMapping("/tenant-jobs")
    public ResponseEntity<ApiResponse<Page<TenantProvisioningJobResponse>>> getJobs(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) TenantProvisioningJob.ProvisioningJobStatus status,
            @RequestParam(required = false) TenantProvisioningJob.ProvisioningJobType jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {
        Page<TenantProvisioningJobResponse> response = tenantProvisioningJobService.getJobs(
                        tenantId,
                        status,
                        jobType,
                        PageRequest.of(page, size, parseSort(sort)))
                .map(TenantProvisioningJobResponse::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tenant-jobs/{id}")
    public ResponseEntity<ApiResponse<TenantProvisioningJobResponse>> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                TenantProvisioningJobResponse.fromEntity(tenantProvisioningJobService.getJobById(id))
        ));
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "id");
        }

        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElse(Sort.Direction.DESC);
            return Sort.by(direction, parts[0].trim());
        }
        return Sort.by(Sort.Direction.DESC, sort.trim());
    }
}
