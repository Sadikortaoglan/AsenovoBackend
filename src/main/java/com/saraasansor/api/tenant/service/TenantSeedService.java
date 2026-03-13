package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.ValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TenantSeedService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SchemaManagementService schemaManagementService;

    public TenantSeedService(JdbcTemplate jdbcTemplate,
                             PasswordEncoder passwordEncoder,
                             SchemaManagementService schemaManagementService) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.schemaManagementService = schemaManagementService;
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
}
