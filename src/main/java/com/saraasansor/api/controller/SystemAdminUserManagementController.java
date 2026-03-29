package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitLookupDto;
import com.saraasansor.api.dto.ManagedUserResponse;
import com.saraasansor.api.dto.PlatformUserCreateRequest;
import com.saraasansor.api.dto.PlatformUserUpdateRequest;
import com.saraasansor.api.dto.PlatformTenantUserResetPasswordRequest;
import com.saraasansor.api.dto.TenantContextSwitchResponse;
import com.saraasansor.api.dto.TenantUserCreateRequest;
import com.saraasansor.api.dto.TenantUserUpdateRequest;
import com.saraasansor.api.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.util.List;

@RestController
@RequestMapping("/system-admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class SystemAdminUserManagementController {

    private final UserManagementService userManagementService;

    public SystemAdminUserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/platform-users")
    public ResponseEntity<ApiResponse<List<ManagedUserResponse>>> getPlatformUsers() {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getPlatformUsers()));
    }

    @PostMapping("/platform-users")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> createPlatformUser(
            @Valid @RequestBody PlatformUserCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Platform user created", userManagementService.createPlatformUser(request)));
    }

    @PutMapping("/platform-users/{id}")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> updatePlatformUser(
            @PathVariable Long id,
            @RequestBody PlatformUserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Platform user updated", userManagementService.updatePlatformUser(id, request)));
    }

    @PostMapping("/platform-users/{id}/disable")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> disablePlatformUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Platform user disabled", userManagementService.setPlatformUserEnabled(id, false)));
    }

    @PostMapping("/platform-users/{id}/enable")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> enablePlatformUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Platform user enabled", userManagementService.setPlatformUserEnabled(id, true)));
    }

    @PostMapping("/tenants/{tenantId}/context-switch")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<TenantContextSwitchResponse>> switchTenantContext(@PathVariable Long tenantId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant context selected", userManagementService.switchTenantContext(tenantId)));
    }

    @GetMapping("/tenants/{tenantId}/users")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<Page<ManagedUserResponse>>> getTenantUsers(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getTenantUsersForPlatform(
                tenantId,
                query,
                role,
                enabled,
                PageRequest.of(page, size, parseSort(sort))
        )));
    }

    @GetMapping("/tenants/{tenantId}/b2b-units/lookup")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<List<B2BUnitLookupDto>>> lookupTenantB2BUnits(
            @PathVariable Long tenantId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getTenantB2BUnitLookupForPlatform(tenantId, query)));
    }

    @GetMapping("/tenants/{tenantId}/users/{userId}")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> getTenantUser(
            @PathVariable Long tenantId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getTenantUserForPlatform(tenantId, userId)));
    }

    @PostMapping("/tenants/{tenantId}/users")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> createTenantUser(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantUserCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user created", userManagementService.createTenantUserForPlatform(tenantId, request)));
    }

    @PutMapping("/tenants/{tenantId}/users/{userId}")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> updateTenantUser(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @RequestBody TenantUserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user updated", userManagementService.updateTenantUserForPlatform(tenantId, userId, request)));
    }

    @PostMapping("/tenants/{tenantId}/users/{userId}/disable")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> disableTenantUser(
            @PathVariable Long tenantId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user disabled", userManagementService.setTenantUserEnabledForPlatform(tenantId, userId, false)));
    }

    @PostMapping("/tenants/{tenantId}/users/{userId}/enable")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> enableTenantUser(
            @PathVariable Long tenantId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user enabled", userManagementService.setTenantUserEnabledForPlatform(tenantId, userId, true)));
    }

    @PostMapping("/tenants/{tenantId}/users/{userId}/reset-password")
    @PreAuthorize("denyAll()")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> resetTenantUserPassword(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @Valid @RequestBody PlatformTenantUserResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tenant user password reset",
                userManagementService.resetTenantUserPasswordForPlatform(tenantId, userId, request)
        ));
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElse(Sort.Direction.ASC);
            return Sort.by(direction, parts[0].trim());
        }
        return Sort.by(Sort.Direction.ASC, sort.trim());
    }
}
