package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserRoleMappingTest {

    @Test
    void shouldMapLegacySystemAdminToPlatformAdminCanonicalRole() {
        assertThat(User.Role.SYSTEM_ADMIN.toCanonical()).isEqualTo(User.Role.PLATFORM_ADMIN);
        assertThat(User.Role.PLATFORM_ADMIN.toPersistenceRole()).isEqualTo(User.Role.SYSTEM_ADMIN);
    }

    @Test
    void shouldMapLegacyStaffAdminToTenantAdminCanonicalRole() {
        assertThat(User.Role.STAFF_ADMIN.toCanonical()).isEqualTo(User.Role.TENANT_ADMIN);
        assertThat(User.Role.TENANT_ADMIN.toPersistenceRole()).isEqualTo(User.Role.STAFF_ADMIN);
    }

    @Test
    void shouldParseLegacyAndPrefixedExternalRoleNames() {
        assertThat(User.Role.fromExternalName("system_admin")).isEqualTo(User.Role.SYSTEM_ADMIN);
        assertThat(User.Role.fromExternalName("ROLE_staff_admin")).isEqualTo(User.Role.STAFF_ADMIN);
        assertThat(User.Role.fromExternalName("ROLE_PLATFORM_ADMIN")).isEqualTo(User.Role.PLATFORM_ADMIN);
    }
}
