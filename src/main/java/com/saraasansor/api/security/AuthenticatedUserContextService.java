package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticatedUserContextService {

    private final UserRepository userRepository;

    public AuthenticatedUserContextService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthenticatedUserContext requireContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String username = authentication.getName();
        User.Role role = resolveRoleFromAuthorities(authentication)
                .orElseThrow(() -> new RuntimeException("User role cannot be resolved"));

        AuthenticatedUserContext context = new AuthenticatedUserContext();
        context.setUsername(username);
        context.setRole(role);

        TenantDescriptor tenant = TenantContext.getCurrentTenant();
        if (tenant != null) {
            context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.TENANT);
            context.setTenantId(tenant.getId());
            context.setTenantSchema(tenant.getSchemaName());
        } else {
            context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.PLATFORM);
        }

        userRepository.findByUsername(username).ifPresent(user -> {
            context.setUserId(user.getId());
            context.setLinkedB2bUnitId(user.getB2bUnit() != null ? user.getB2bUnit().getId() : null);
        });

        return context;
    }

    private Optional<User.Role> resolveRoleFromAuthorities(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if ("ROLE_PLATFORM_ADMIN".equals(value)) {
                return Optional.of(User.Role.PLATFORM_ADMIN);
            }
            if ("ROLE_TENANT_ADMIN".equals(value) || "ROLE_STAFF_ADMIN".equals(value)) {
                return Optional.of(User.Role.TENANT_ADMIN);
            }
            if ("ROLE_STAFF_USER".equals(value)) {
                return Optional.of(User.Role.STAFF_USER);
            }
            if ("ROLE_CARI_USER".equals(value)) {
                return Optional.of(User.Role.CARI_USER);
            }
        }
        return Optional.empty();
    }
}
