package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceScopeTest {

    @AfterEach
    void cleanTenantContext() {
        TenantContext.clear();
    }

    @Test
    void shouldAllowPlatformUserInTenantScopeWhenTenantLocalPlatformAdminExists() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setUsername("platform_admin");
        user.setPasswordHash("hash");
        user.setRole(User.Role.SYSTEM_ADMIN);
        user.setActive(true);
        user.setEnabled(true);
        user.setLocked(false);
        when(userRepository.findByUsername("platform_admin")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        TenantContext.setCurrentTenant(new TenantDescriptor(
                1L, "Acme", "acme", Tenant.TenancyMode.SHARED_SCHEMA, "tenant_acme",
                null, null, null, null, "tenant:acme", "BASIC"
        ));

        UserDetails details = service.loadUserByUsername("platform_admin");
        assertThat(details.getUsername()).isEqualTo("platform_admin");
        assertThat(details.getAuthorities().stream().map(a -> a.getAuthority()))
                .contains("ROLE_PLATFORM_ADMIN");
    }

    @Test
    void shouldRejectTenantUserInPlatformScope() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setUsername("tenant_admin");
        user.setPasswordHash("hash");
        user.setRole(User.Role.STAFF_ADMIN);
        user.setActive(true);
        user.setEnabled(true);
        user.setLocked(false);
        when(userRepository.findByUsername("tenant_admin")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("tenant_admin"))
                .hasMessageContaining("Tenant users must authenticate from tenant scope");
    }

    @Test
    void shouldGrantCanonicalAndLegacyCompatAuthoritiesForLegacyPlatformRole() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = new User();
        user.setUsername("legacy_system_admin");
        user.setPasswordHash("hash");
        user.setRole(User.Role.SYSTEM_ADMIN);
        user.setActive(true);
        user.setEnabled(true);
        user.setLocked(false);
        when(userRepository.findByUsername("legacy_system_admin")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);

        UserDetails details = service.loadUserByUsername("legacy_system_admin");
        Set<String> authorities = details.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(authorities).contains("ROLE_PLATFORM_ADMIN");
        assertThat(authorities).contains("ROLE_SYSTEM_ADMIN");
    }
}
