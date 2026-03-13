package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ManagedUserResponse;
import com.saraasansor.api.dto.TenantUserCreateRequest;
import com.saraasansor.api.dto.TenantUserUpdateRequest;
import com.saraasansor.api.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tenant-admin")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class TenantAdminUserManagementController {

    private final UserManagementService userManagementService;

    public TenantAdminUserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<ManagedUserResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success(userManagementService.getTenantUsersForTenantAdmin()));
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
}
