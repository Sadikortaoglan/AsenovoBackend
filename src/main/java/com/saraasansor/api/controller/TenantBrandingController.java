package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.tenant.dto.TenantBrandingResponseDTO;
import com.saraasansor.api.tenant.dto.TenantBrandingUpdateRequestDTO;
import com.saraasansor.api.tenant.service.TenantBrandingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tenant/branding")
public class TenantBrandingController {

    private final TenantBrandingService tenantBrandingService;

    public TenantBrandingController(TenantBrandingService tenantBrandingService) {
        this.tenantBrandingService = tenantBrandingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TenantBrandingResponseDTO>> getBranding() {
        TenantBrandingResponseDTO response = tenantBrandingService.getCurrentTenantBranding();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<TenantBrandingResponseDTO>> updateBranding(
            @Valid @RequestBody TenantBrandingUpdateRequestDTO requestDTO) {
        TenantBrandingResponseDTO response = tenantBrandingService.updateBranding(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Tenant branding updated", response));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<TenantBrandingResponseDTO>> updateLogo(
            @RequestPart("file") MultipartFile file) {
        TenantBrandingResponseDTO response = tenantBrandingService.updateLogo(file);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Tenant logo updated", response));
    }
}
