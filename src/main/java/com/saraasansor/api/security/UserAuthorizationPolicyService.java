package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class UserAuthorizationPolicyService {

    public void requirePlatformAdmin(AuthenticatedUserContext actor) {
        if (actor == null || actor.getRole() == null || !actor.getRole().isPlatformAdmin()) {
            throw new AccessDeniedException("Only PLATFORM_ADMIN can perform this action");
        }
    }

    public void requireTenantAdmin(AuthenticatedUserContext actor) {
        if (actor == null || actor.getRole() == null || !actor.getRole().isTenantAdmin()) {
            throw new AccessDeniedException("Only TENANT_ADMIN can perform this action");
        }
    }

    public void requireTenantScope(AuthenticatedUserContext actor) {
        if (actor == null || actor.getAuthScopeType() != AuthenticatedUserContext.AuthScopeType.TENANT) {
            throw new AccessDeniedException("Tenant context is required");
        }
    }

    public boolean requireTenantScopedUserManager(AuthenticatedUserContext actor) {
        requireTenantScope(actor);
        if (actor == null || actor.getRole() == null) {
            throw new AccessDeniedException("User role cannot be resolved");
        }

        User.Role canonicalRole = actor.getRole().toCanonical();
        if (canonicalRole.isPlatformAdmin()) {
            return true;
        }
        if (canonicalRole.isTenantAdmin()) {
            return false;
        }
        throw new AccessDeniedException("Only TENANT_ADMIN or tenant-scoped PLATFORM_ADMIN can perform this action");
    }

    public User.Role normalizeRequestedRole(User.Role requestedRole) {
        if (requestedRole == null) {
            throw new RuntimeException("Role is required");
        }
        return requestedRole.toCanonical();
    }

    public void assertPlatformUserRole(User.Role requestedRole) {
        User.Role canonical = normalizeRequestedRole(requestedRole);
        if (!canonical.isPlatformAdmin()) {
            throw new AccessDeniedException("Platform user role must be PLATFORM_ADMIN");
        }
    }

    public void assertTenantRoleForPlatform(User.Role requestedRole) {
        User.Role canonical = normalizeRequestedRole(requestedRole);
        if (canonical.isPlatformAdmin()) {
            throw new AccessDeniedException("PLATFORM_ADMIN role cannot be assigned in tenant scope");
        }
    }

    public void assertTenantRoleForTenantAdmin(User.Role requestedRole) {
        User.Role canonical = normalizeRequestedRole(requestedRole);
        if (canonical == User.Role.TENANT_ADMIN || canonical.isPlatformAdmin()) {
            throw new AccessDeniedException("TENANT_ADMIN can only manage STAFF_USER and CARI_USER");
        }
    }
}
