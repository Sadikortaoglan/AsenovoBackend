package com.saraasansor.api.service;

import com.saraasansor.api.dto.UserSelfPasswordChangeRequest;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.security.AuthenticatedUserContext;
import com.saraasansor.api.security.AuthenticatedUserContextService;
import com.saraasansor.api.security.UserAuthorizationPolicyService;
import com.saraasansor.api.util.AuditLogger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserSelfPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticatedUserContextService authenticatedUserContextService;
    private final UserAuthorizationPolicyService authorizationPolicyService;
    private final AuditLogger auditLogger;

    public UserSelfPasswordService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   AuthenticatedUserContextService authenticatedUserContextService,
                                   UserAuthorizationPolicyService authorizationPolicyService,
                                   AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticatedUserContextService = authenticatedUserContextService;
        this.authorizationPolicyService = authorizationPolicyService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public void changeOwnPassword(UserSelfPasswordChangeRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requireTenantScope(actor);

        String currentPassword = normalizeRequired(request != null ? request.resolveCurrentPassword() : null, "currentPassword");
        String newPassword = normalizeRequired(request != null ? request.resolveNewPassword() : null, "newPassword");
        String confirmPassword = normalizeRequired(request != null ? request.resolveConfirmPassword() : null, "confirmPassword");

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("newPassword and confirmPassword must match");
        }
        if (newPassword.equals(currentPassword)) {
            throw new RuntimeException("newPassword must be different from currentPassword");
        }

        User user = userRepository.findByUsername(actor.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + actor.getUsername()));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLocked(false);
        User saved = userRepository.save(user);
        auditLogger.log("TENANT_USER_SELF_PASSWORD_CHANGED", "USER", saved.getId(), metadata(actor, saved));
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " is required");
        }
        return value.trim();
    }

    private Map<String, Object> metadata(AuthenticatedUserContext actor, User targetUser) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actorUserId", actor != null ? actor.getUserId() : null);
        metadata.put("actorUsername", actor != null ? actor.getUsername() : null);
        metadata.put("actorRole", actor != null && actor.getRole() != null ? actor.getRole().name() : null);
        metadata.put("authScopeType", actor != null && actor.getAuthScopeType() != null ? actor.getAuthScopeType().name() : null);
        metadata.put("tenantId", actor != null ? actor.getTenantId() : null);
        metadata.put("targetUserId", targetUser != null ? targetUser.getId() : null);
        metadata.put("targetUsername", targetUser != null ? targetUser.getUsername() : null);
        metadata.put("targetRole", targetUser != null && targetUser.getCanonicalRole() != null ? targetUser.getCanonicalRole().name() : null);
        return metadata;
    }
}
