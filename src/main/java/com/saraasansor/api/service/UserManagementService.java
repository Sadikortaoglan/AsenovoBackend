package com.saraasansor.api.service;

import com.saraasansor.api.dto.ManagedUserResponse;
import com.saraasansor.api.dto.PlatformUserCreateRequest;
import com.saraasansor.api.dto.PlatformUserUpdateRequest;
import com.saraasansor.api.dto.TenantContextSwitchResponse;
import com.saraasansor.api.dto.TenantUserCreateRequest;
import com.saraasansor.api.dto.TenantUserUpdateRequest;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.security.AuthenticatedUserContext;
import com.saraasansor.api.security.AuthenticatedUserContextService;
import com.saraasansor.api.security.UserAuthorizationPolicyService;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.service.TenantContextExecutionService;
import com.saraasansor.api.util.AuditLogger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class UserManagementService {

    private static final List<User.Role> PLATFORM_PERSISTED_ROLES = List.of(User.Role.SYSTEM_ADMIN, User.Role.PLATFORM_ADMIN);
    private static final List<User.Role> TENANT_EXCLUDED_ROLES = List.of(User.Role.SYSTEM_ADMIN, User.Role.PLATFORM_ADMIN);

    private final UserRepository userRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantContextExecutionService tenantContextExecutionService;
    private final AuthenticatedUserContextService authenticatedUserContextService;
    private final UserAuthorizationPolicyService authorizationPolicyService;
    private final AuditLogger auditLogger;

    public UserManagementService(UserRepository userRepository,
                                 B2BUnitRepository b2bUnitRepository,
                                 PasswordEncoder passwordEncoder,
                                 TenantContextExecutionService tenantContextExecutionService,
                                 AuthenticatedUserContextService authenticatedUserContextService,
                                 UserAuthorizationPolicyService authorizationPolicyService,
                                 AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantContextExecutionService = tenantContextExecutionService;
        this.authenticatedUserContextService = authenticatedUserContextService;
        this.authorizationPolicyService = authorizationPolicyService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<ManagedUserResponse> getPlatformUsers() {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        return userRepository.findByRoleInOrderByIdAsc(PLATFORM_PERSISTED_ROLES)
                .stream()
                .map(ManagedUserResponse::fromEntity)
                .toList();
    }

    public ManagedUserResponse createPlatformUser(PlatformUserCreateRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        String username = normalizeRequired(request.getUsername(), "username");
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(normalizeRequired(request.getPassword(), "password")));
        user.setRole(User.Role.PLATFORM_ADMIN.toPersistenceRole());
        user.setUserType(User.UserType.SYSTEM_ADMIN);
        boolean enabled = request.getActive() == null || request.getActive();
        user.setActive(enabled);
        user.setEnabled(enabled);
        user.setLocked(false);

        User saved = userRepository.save(user);
        auditLogger.log("PLATFORM_USER_CREATED", "USER", saved.getId(), metadata(actor, saved, null));
        return ManagedUserResponse.fromEntity(saved);
    }

    public ManagedUserResponse updatePlatformUser(Long id, PlatformUserUpdateRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        User user = userRepository.findByIdAndRoleIn(id, PLATFORM_PERSISTED_ROLES)
                .orElseThrow(() -> new NotFoundException("Platform user not found: " + id));

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String normalizedUsername = request.getUsername().trim();
            if (!normalizedUsername.equals(user.getUsername())
                    && userRepository.existsByUsernameAndIdNot(normalizedUsername, id)) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(normalizedUsername);
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (request.getLocked() != null) {
            user.setLocked(request.getLocked());
        }

        applyActiveFlagForPlatformUser(user, request.getActive());
        User saved = userRepository.save(user);
        auditLogger.log("PLATFORM_USER_UPDATED", "USER", saved.getId(), metadata(actor, saved, null));
        return ManagedUserResponse.fromEntity(saved);
    }

    public ManagedUserResponse setPlatformUserEnabled(Long id, boolean enabled) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        User user = userRepository.findByIdAndRoleIn(id, PLATFORM_PERSISTED_ROLES)
                .orElseThrow(() -> new NotFoundException("Platform user not found: " + id));

        applyActiveFlagForPlatformUser(user, enabled);
        User saved = userRepository.save(user);
        String action = enabled ? "PLATFORM_USER_ENABLED" : "PLATFORM_USER_DISABLED";
        auditLogger.log(action, "USER", saved.getId(), metadata(actor, saved, null));
        return ManagedUserResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TenantContextSwitchResponse switchTenantContext(Long tenantId) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        TenantDescriptor tenant = tenantContextExecutionService.resolveTenantContext(tenantId);
        auditLogger.log("PLATFORM_TENANT_CONTEXT_SWITCH", "TENANT", tenant.getId(), metadata(actor, null, tenant.getId()));
        return TenantContextSwitchResponse.fromDescriptor(tenant);
    }

    @Transactional(readOnly = true)
    public List<ManagedUserResponse> getTenantUsersForPlatform(Long tenantId) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        return tenantContextExecutionService.executeInTenantContext(tenantId, () ->
                userRepository.findByRoleNotInOrderByIdAsc(TENANT_EXCLUDED_ROLES).stream()
                        .map(ManagedUserResponse::fromEntity)
                        .toList());
    }

    public ManagedUserResponse createTenantUserForPlatform(Long tenantId, TenantUserCreateRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        return tenantContextExecutionService.executeInTenantContext(tenantId, () -> {
            User.Role requestedCanonicalRole = authorizationPolicyService.normalizeRequestedRole(request.getRole());
            authorizationPolicyService.assertTenantRoleForPlatform(requestedCanonicalRole);
            return createTenantUserInternal(request, requestedCanonicalRole, actor, tenantId, "TENANT_USER_CREATED_BY_PLATFORM");
        });
    }

    public ManagedUserResponse updateTenantUserForPlatform(Long tenantId, Long userId, TenantUserUpdateRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        return tenantContextExecutionService.executeInTenantContext(tenantId, () ->
                updateTenantUserInternal(userId, request, actor, tenantId, true));
    }

    public ManagedUserResponse setTenantUserEnabledForPlatform(Long tenantId, Long userId, boolean enabled) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requirePlatformAdmin(actor);

        return tenantContextExecutionService.executeInTenantContext(tenantId, () ->
                setTenantUserEnabledInternal(userId, enabled, actor, tenantId, true));
    }

    @Transactional(readOnly = true)
    public Page<ManagedUserResponse> getTenantUsersForTenantAdmin(String query,
                                                                  String roleFilter,
                                                                  Boolean enabledFilter,
                                                                  Pageable pageable) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requireTenantAdmin(actor);
        authorizationPolicyService.requireTenantScope(actor);

        String normalizedQuery = normalizeNullable(query);
        String queryValue = normalizedQuery != null ? normalizedQuery : "";
        User.Role persistedRoleFilter = normalizeTenantRoleFilter(roleFilter);
        return userRepository.searchTenantUsers(
                        queryValue,
                        persistedRoleFilter,
                        enabledFilter,
                        TENANT_EXCLUDED_ROLES,
                        pageable)
                .map(ManagedUserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public ManagedUserResponse getTenantUserForTenantAdmin(Long userId) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requireTenantAdmin(actor);
        authorizationPolicyService.requireTenantScope(actor);

        User user = findManageableTenantUserById(userId);
        return ManagedUserResponse.fromEntity(user);
    }

    public ManagedUserResponse createTenantUserForTenantAdmin(TenantUserCreateRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requireTenantAdmin(actor);
        authorizationPolicyService.requireTenantScope(actor);

        User.Role requestedCanonicalRole = authorizationPolicyService.normalizeRequestedRole(request.getRole());
        authorizationPolicyService.assertTenantRoleForTenantAdmin(requestedCanonicalRole);
        return createTenantUserInternal(request, requestedCanonicalRole, actor, actor.getTenantId(), "TENANT_USER_CREATED_BY_TENANT_ADMIN");
    }

    public ManagedUserResponse updateTenantUserForTenantAdmin(Long userId, TenantUserUpdateRequest request) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requireTenantAdmin(actor);
        authorizationPolicyService.requireTenantScope(actor);

        return updateTenantUserInternal(userId, request, actor, actor.getTenantId(), false);
    }

    public ManagedUserResponse setTenantUserEnabledForTenantAdmin(Long userId, boolean enabled) {
        AuthenticatedUserContext actor = authenticatedUserContextService.requireContext();
        authorizationPolicyService.requireTenantAdmin(actor);
        authorizationPolicyService.requireTenantScope(actor);

        return setTenantUserEnabledInternal(userId, enabled, actor, actor.getTenantId(), false);
    }

    private ManagedUserResponse createTenantUserInternal(TenantUserCreateRequest request,
                                                         User.Role requestedCanonicalRole,
                                                         AuthenticatedUserContext actor,
                                                         Long tenantId,
                                                         String auditAction) {
        String username = normalizeRequired(request.getUsername(), "username");
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(normalizeRequired(request.getPassword(), "password")));
        user.setRole(requestedCanonicalRole.toPersistenceRole());
        user.setUserType(resolveUserType(requestedCanonicalRole));
        user.setB2bUnit(resolveB2BUnitForCreate(requestedCanonicalRole, request.resolveLinkedB2bUnitId()));
        Boolean requestedEnabled = request.resolveEnabledValue();
        boolean enabled = requestedEnabled == null || requestedEnabled;
        user.setActive(enabled);
        user.setEnabled(enabled);
        user.setLocked(false);

        User saved = userRepository.save(user);
        auditLogger.log(auditAction, "USER", saved.getId(), metadata(actor, saved, tenantId));
        return ManagedUserResponse.fromEntity(saved);
    }

    private ManagedUserResponse updateTenantUserInternal(Long userId,
                                                         TenantUserUpdateRequest request,
                                                         AuthenticatedUserContext actor,
                                                         Long tenantId,
                                                         boolean allowTenantAdminRoleByPlatform) {
        User user = findManageableTenantUserById(userId);

        User.Role existingCanonicalRole = user.getCanonicalRole();
        if (!allowTenantAdminRoleByPlatform && existingCanonicalRole == User.Role.TENANT_ADMIN) {
            throw new RuntimeException("TENANT_ADMIN cannot be updated by TENANT_ADMIN");
        }

        Long previousLinkedB2bUnitId = user.getB2bUnit() != null ? user.getB2bUnit().getId() : null;

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String normalizedUsername = request.getUsername().trim();
            if (!normalizedUsername.equals(user.getUsername())
                    && userRepository.existsByUsernameAndIdNot(normalizedUsername, userId)) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(normalizedUsername);
        }

        User.Role targetCanonicalRole = existingCanonicalRole;
        if (request.getRole() != null) {
            targetCanonicalRole = authorizationPolicyService.normalizeRequestedRole(request.getRole());
            if (allowTenantAdminRoleByPlatform) {
                authorizationPolicyService.assertTenantRoleForPlatform(targetCanonicalRole);
            } else {
                authorizationPolicyService.assertTenantRoleForTenantAdmin(targetCanonicalRole);
            }
            user.setRole(targetCanonicalRole.toPersistenceRole());
            user.setUserType(resolveUserType(targetCanonicalRole));
        }

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (request.getLocked() != null) {
            user.setLocked(request.getLocked());
        }

        user.setB2bUnit(resolveB2BUnitForUpdate(targetCanonicalRole, request.resolveLinkedB2bUnitId(), user.getB2bUnit()));

        Boolean requestedEnabled = request.resolveEnabledValue();
        if (requestedEnabled != null) {
            user.setActive(requestedEnabled);
            user.setEnabled(requestedEnabled);
        }

        User saved = userRepository.save(user);
        String action = allowTenantAdminRoleByPlatform
                ? "TENANT_USER_UPDATED_BY_PLATFORM"
                : "TENANT_USER_UPDATED_BY_TENANT_ADMIN";
        auditLogger.log(action, "USER", saved.getId(), metadata(actor, saved, tenantId));

        Long newLinkedB2bUnitId = saved.getB2bUnit() != null ? saved.getB2bUnit().getId() : null;
        if (!Objects.equals(previousLinkedB2bUnitId, newLinkedB2bUnitId)) {
            String linkageAction = allowTenantAdminRoleByPlatform
                    ? "TENANT_USER_B2BUNIT_LINK_CHANGED_BY_PLATFORM"
                    : "TENANT_USER_B2BUNIT_LINK_CHANGED_BY_TENANT_ADMIN";
            auditLogger.log(linkageAction, "USER", saved.getId(), metadata(actor, saved, tenantId));
        }
        return ManagedUserResponse.fromEntity(saved);
    }

    private ManagedUserResponse setTenantUserEnabledInternal(Long userId,
                                                             boolean enabled,
                                                             AuthenticatedUserContext actor,
                                                             Long tenantId,
                                                             boolean allowTenantAdminRoleByPlatform) {
        User user = findManageableTenantUserById(userId);

        User.Role canonicalRole = user.getCanonicalRole();
        if (!allowTenantAdminRoleByPlatform && canonicalRole == User.Role.TENANT_ADMIN) {
            throw new RuntimeException("TENANT_ADMIN cannot be managed by TENANT_ADMIN");
        }

        user.setActive(enabled);
        user.setEnabled(enabled);
        User saved = userRepository.save(user);

        String action;
        if (allowTenantAdminRoleByPlatform) {
            action = enabled ? "TENANT_USER_ENABLED_BY_PLATFORM" : "TENANT_USER_DISABLED_BY_PLATFORM";
        } else {
            action = enabled ? "TENANT_USER_ENABLED_BY_TENANT_ADMIN" : "TENANT_USER_DISABLED_BY_TENANT_ADMIN";
        }
        auditLogger.log(action, "USER", saved.getId(), metadata(actor, saved, tenantId));
        return ManagedUserResponse.fromEntity(saved);
    }

    private void applyActiveFlagForPlatformUser(User user, Boolean activeValue) {
        if (activeValue == null) {
            return;
        }

        if (!activeValue && Boolean.TRUE.equals(user.getActive())) {
            long activePlatformCount = userRepository.countByRoleInAndActiveTrue(PLATFORM_PERSISTED_ROLES);
            if (activePlatformCount <= 1) {
                throw new RuntimeException("At least one active PLATFORM_ADMIN is required");
            }
        }
        user.setActive(activeValue);
        user.setEnabled(activeValue);
    }

    private User.UserType resolveUserType(User.Role canonicalRole) {
        if (canonicalRole == null) {
            return User.UserType.STAFF;
        }
        if (canonicalRole.isPlatformAdmin()) {
            return User.UserType.SYSTEM_ADMIN;
        }
        if (canonicalRole.isCariUser()) {
            return User.UserType.CARI;
        }
        return User.UserType.STAFF;
    }

    private B2BUnit resolveB2BUnitForCreate(User.Role canonicalRole, Long linkedB2bUnitId) {
        if (canonicalRole != null && canonicalRole.isCariUser()) {
            if (linkedB2bUnitId == null) {
                throw new RuntimeException("linkedB2bUnitId is required for CARI_USER");
            }
            return b2bUnitRepository.findByIdAndActiveTrue(linkedB2bUnitId)
                    .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        }

        if (linkedB2bUnitId != null) {
            throw new RuntimeException("linkedB2bUnitId can only be set for CARI_USER");
        }

        return null;
    }

    private B2BUnit resolveB2BUnitForUpdate(User.Role canonicalRole,
                                            Long linkedB2bUnitId,
                                            B2BUnit existingB2bUnit) {
        if (canonicalRole != null && canonicalRole.isCariUser()) {
            if (linkedB2bUnitId != null) {
                return b2bUnitRepository.findByIdAndActiveTrue(linkedB2bUnitId)
                        .orElseThrow(() -> new RuntimeException("B2B unit not found"));
            }
            if (existingB2bUnit != null) {
                return existingB2bUnit;
            }
            throw new RuntimeException("linkedB2bUnitId is required for CARI_USER");
        }

        if (linkedB2bUnitId != null) {
            throw new RuntimeException("linkedB2bUnitId can only be set for CARI_USER");
        }

        return null;
    }

    private User findManageableTenantUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Tenant user not found: " + userId));
        if (user.getCanonicalRole() != null && user.getCanonicalRole().isPlatformAdmin()) {
            throw new NotFoundException("Tenant user not found: " + userId);
        }
        return user;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private User.Role normalizeTenantRoleFilter(String roleFilter) {
        String normalizedRole = normalizeNullable(roleFilter);
        if (normalizedRole == null) {
            return null;
        }

        User.Role canonicalRole;
        try {
            canonicalRole = User.Role.fromExternalName(normalizedRole).toCanonical();
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid role filter: " + roleFilter);
        }

        if (canonicalRole.isPlatformAdmin()) {
            throw new RuntimeException("PLATFORM_ADMIN cannot be used as tenant user role filter");
        }
        return canonicalRole.toPersistenceRole();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " is required");
        }
        return value.trim();
    }

    private Map<String, Object> metadata(AuthenticatedUserContext actor, User targetUser, Long tenantId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("actorUserId", actor != null ? actor.getUserId() : null);
        metadata.put("actorUsername", actor != null ? actor.getUsername() : null);
        metadata.put("actorRole", actor != null && actor.getRole() != null ? actor.getRole().name() : null);
        metadata.put("authScopeType", actor != null && actor.getAuthScopeType() != null ? actor.getAuthScopeType().name() : null);
        metadata.put("tenantId", tenantId);
        metadata.put("targetUserId", targetUser != null ? targetUser.getId() : null);
        metadata.put("targetUsername", targetUser != null ? targetUser.getUsername() : null);
        metadata.put("targetRole", targetUser != null && targetUser.getCanonicalRole() != null ? targetUser.getCanonicalRole().name() : null);
        metadata.put("targetLinkedB2bUnitId", targetUser != null && targetUser.getB2bUnit() != null ? targetUser.getB2bUnit().getId() : null);
        metadata.put("targetLinkedB2bUnitName", targetUser != null && targetUser.getB2bUnit() != null ? targetUser.getB2bUnit().getName() : null);
        return metadata;
    }
}
