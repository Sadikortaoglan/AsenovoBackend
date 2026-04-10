package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.SelfPasswordChangeRequest;
import com.saraasansor.api.dto.UserSelfPasswordChangeRequest;
import com.saraasansor.api.service.UserManagementService;
import com.saraasansor.api.service.UserSelfPasswordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeController {

    private final UserManagementService userManagementService;
    private final UserSelfPasswordService userSelfPasswordService;

    public MeController(UserManagementService userManagementService,
                        UserSelfPasswordService userSelfPasswordService) {
        this.userManagementService = userManagementService;
        this.userSelfPasswordService = userSelfPasswordService;
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody SelfPasswordChangeRequest request) {
        userManagementService.changeOwnPasswordForTenantPlatformAdmin(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @PostMapping("/change-own-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changeOwnPassword(@Valid @RequestBody UserSelfPasswordChangeRequest request) {
        userSelfPasswordService.changeOwnPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }
}
