package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantSeedServiceTest {

    @Test
    void shouldCreateTenantLocalPlatformAdminWhenMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SchemaManagementService schemaManagementService = mock(SchemaManagementService.class);
        TenantSeedService service = new TenantSeedService(jdbcTemplate, passwordEncoder, schemaManagementService);

        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminEnabled", true);
        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminUsername", "tenant_platform_admin");
        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminPassword", "Bootstrap123");

        when(schemaManagementService.sanitizeSchemaName("tenant_5")).thenReturn("tenant_5");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(0L);
        when(passwordEncoder.encode("Bootstrap123")).thenReturn("ENCODED");
        when(jdbcTemplate.update(anyString(), eq("tenant_platform_admin"), eq("ENCODED"))).thenReturn(1);

        boolean created = service.seedTenantLocalPlatformAdmin("tenant_5");

        assertThat(created).isTrue();
        verify(jdbcTemplate).update(anyString(), eq("tenant_platform_admin"), eq("ENCODED"));
    }

    @Test
    void shouldNotCreateDuplicateTenantLocalPlatformAdmin() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SchemaManagementService schemaManagementService = mock(SchemaManagementService.class);
        TenantSeedService service = new TenantSeedService(jdbcTemplate, passwordEncoder, schemaManagementService);

        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminEnabled", true);
        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminUsername", "tenant_platform_admin");
        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminPassword", "Bootstrap123");

        when(schemaManagementService.sanitizeSchemaName("tenant_5")).thenReturn("tenant_5");
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        boolean created = service.seedTenantLocalPlatformAdmin("tenant_5");

        assertThat(created).isFalse();
        verify(jdbcTemplate, never()).update(anyString(), anyString(), anyString());
    }

    @Test
    void shouldFailWhenBootstrapCredentialsAreMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        SchemaManagementService schemaManagementService = mock(SchemaManagementService.class);
        TenantSeedService service = new TenantSeedService(jdbcTemplate, passwordEncoder, schemaManagementService);

        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminEnabled", true);
        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminUsername", "");
        ReflectionTestUtils.setField(service, "tenantBootstrapPlatformAdminPassword", "");

        assertThatThrownBy(() -> service.seedTenantLocalPlatformAdmin("tenant_5"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("TENANT_BOOTSTRAP_PLATFORM_ADMIN_USERNAME");
    }
}
