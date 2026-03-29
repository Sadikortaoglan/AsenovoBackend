package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TenantSeedService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SchemaManagementService schemaManagementService;

    @Value("${TENANT_BOOTSTRAP_PLATFORM_ADMIN_ENABLED:true}")
    private boolean tenantBootstrapPlatformAdminEnabled;

    @Value("${TENANT_BOOTSTRAP_PLATFORM_ADMIN_USERNAME:${APP_PLATFORM_ADMIN_USERNAME:${PLATFORM_ADMIN_USERNAME:}}}")
    private String tenantBootstrapPlatformAdminUsername;

    @Value("${TENANT_BOOTSTRAP_PLATFORM_ADMIN_PASSWORD:${APP_PLATFORM_ADMIN_PASSWORD:${PLATFORM_ADMIN_PASSWORD:}}}")
    private String tenantBootstrapPlatformAdminPassword;

    public TenantSeedService(JdbcTemplate jdbcTemplate,
                             PasswordEncoder passwordEncoder,
                             SchemaManagementService schemaManagementService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.schemaManagementService = schemaManagementService;
    }

    public boolean isTenantLocalPlatformAdminBootstrapEnabled() {
        return tenantBootstrapPlatformAdminEnabled;
    }

    public boolean hasTenantLocalPlatformAdminBootstrapCredentials() {
        return !isBlank(tenantBootstrapPlatformAdminUsername) && !isBlank(tenantBootstrapPlatformAdminPassword);
    }

    public boolean seedTenantLocalPlatformAdmin(String schemaName) {
        if (!tenantBootstrapPlatformAdminEnabled) {
            return false;
        }

        String username = normalizeRequired(tenantBootstrapPlatformAdminUsername, "TENANT_BOOTSTRAP_PLATFORM_ADMIN_USERNAME");
        String rawPassword = normalizeRequired(tenantBootstrapPlatformAdminPassword, "TENANT_BOOTSTRAP_PLATFORM_ADMIN_PASSWORD");
        String safeSchema = schemaManagementService.sanitizeSchemaName(schemaName);

        String countSql = "SELECT COUNT(*) FROM \"" + safeSchema + "\".users " +
                "WHERE UPPER(role) IN ('SYSTEM_ADMIN','PLATFORM_ADMIN')";
        Long existingPlatformAdmins = jdbcTemplate.queryForObject(countSql, Long.class);
        if (existingPlatformAdmins != null && existingPlatformAdmins > 0L) {
            return false;
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        String insertSql = "INSERT INTO \"" + safeSchema + "\".users (" +
                "username, password_hash, role, user_type, active, enabled, locked, created_at, updated_at) " +
                "VALUES (?, ?, 'PLATFORM_ADMIN', 'SYSTEM_ADMIN', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (username) DO NOTHING";
        int inserted = jdbcTemplate.update(insertSql, username, passwordHash);
        return inserted > 0;
    }

    public void seedInitialAdmin(String schemaName, String username, String rawPassword) {
        if (isBlank(username) && isBlank(rawPassword)) {
            return;
        }

        if (isBlank(username) || isBlank(rawPassword)) {
            throw new ValidationException("Initial admin username and password must be provided together");
        }

        String safeSchema = schemaManagementService.sanitizeSchemaName(schemaName);
        String normalizedUsername = username.trim();
        String passwordHash = passwordEncoder.encode(rawPassword.trim());

        String sql = "INSERT INTO \"" + safeSchema + "\".users (" +
                "username, password_hash, role, user_type, active, enabled, locked, created_at, updated_at) " +
                "VALUES (?, ?, 'STAFF_ADMIN', 'STAFF', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (username) DO NOTHING";

        jdbcTemplate.update(sql, normalizedUsername, passwordHash);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new ValidationException(fieldName + " must be provided for tenant platform admin bootstrap");
        }
        return value.trim();
    }
}
