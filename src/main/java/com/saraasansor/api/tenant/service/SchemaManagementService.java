package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.ValidationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class SchemaManagementService {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final JdbcTemplate jdbcTemplate;

    public SchemaManagementService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createSchemaIfNotExists(String schemaName) {
        String safeSchemaName = sanitizeSchemaName(schemaName);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + safeSchemaName + "\"");
    }

    public String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            throw new ValidationException("Schema name is required");
        }
        String normalized = schemaName.trim();
        if (!SCHEMA_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("Schema name contains invalid characters");
        }
        return normalized;
    }
}
