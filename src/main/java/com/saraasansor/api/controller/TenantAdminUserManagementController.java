package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ManagedUserResponse;
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

@RestController
@RequestMapping("/tenant-admin")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','PLATFORM_ADMIN')")
public class TenantAdminUserManagementController {

    private final UserManagementService userManagementService;

    public TenantAdminUserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<ManagedUserResponse>>> getUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getTenantUsersForTenantAdmin(
                query,
                role,
                enabled,
                PageRequest.of(page, size, parseSort(sort))
        )));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getTenantUserForTenantAdmin(id)));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> createUser(
            @Valid @RequestBody TenantUserCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user created", userManagementService.createTenantUserForTenantAdmin(request)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody TenantUserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user updated", userManagementService.updateTenantUserForTenantAdmin(id, request)));
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user disabled", userManagementService.setTenantUserEnabledForTenantAdmin(id, false)));
    }

    @PostMapping("/users/{id}/enable")
    public ResponseEntity<ApiResponse<ManagedUserResponse>> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tenant user enabled", userManagementService.setTenantUserEnabledForTenantAdmin(id, true)));
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
