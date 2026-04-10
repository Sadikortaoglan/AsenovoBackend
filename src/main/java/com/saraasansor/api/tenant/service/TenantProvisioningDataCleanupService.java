package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TenantProvisioningDataCleanupService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Set<String> TABLES_TO_KEEP = Set.of(
            "flyway_schema_history",
            "plans",
            "tenants",
            "subscriptions",
            "features",
            "tenant_feature_overrides",
            "tenant_provisioning_jobs",
            "tenant_provisioning_audit_logs"
    );

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    public TenantProvisioningDataCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean shouldCleanupProvisionedTenantData() {
        return !seedEnabled;
    }

    public void cleanupProvisionedSchema(String schemaName) {
        String safeSchema = sanitizeIdentifier(schemaName, "schemaName");

        String truncateSql = """
                DO $$
                DECLARE
                    target_schema text := '%s';
                    table_list text;
                BEGIN
                    SELECT string_agg(format('%%I.%%I', schemaname, tablename), ', ' ORDER BY tablename)
                    INTO table_list
                    FROM pg_tables
                    WHERE schemaname = target_schema
                      AND tablename NOT IN ('flyway_schema_history', 'users', 'plans', 'tenants', 'subscriptions',
                                            'features', 'tenant_feature_overrides', 'tenant_provisioning_jobs',
                                            'tenant_provisioning_audit_logs');

                    IF table_list IS NOT NULL AND length(table_list) > 0 THEN
                        EXECUTE 'TRUNCATE TABLE ' || table_list || ' RESTART IDENTITY CASCADE';
                    END IF;
                END $$;
                """.formatted(safeSchema);

        jdbcTemplate.execute(truncateSql);
        jdbcTemplate.execute("DELETE FROM \"" + safeSchema + "\".users");
    }

    private String sanitizeIdentifier(String value, String label) {
        if (value == null || value.isBlank() || !IDENTIFIER_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException("Invalid " + label);
        }
        return value.trim();
    }
}
