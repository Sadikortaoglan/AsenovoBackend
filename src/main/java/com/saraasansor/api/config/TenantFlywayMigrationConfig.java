package com.saraasansor.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

@Configuration
@EnableConfigurationProperties(TenantFlywayProperties.class)
public class TenantFlywayMigrationConfig {

    @Bean
    public FlywayMigrationStrategy tenantAwareFlywayMigrationStrategy(TenantFlywayProperties properties) {
        return flyway -> {
            // 1) Control-plane (master/public) migrations
            flyway.migrate();

            if (!properties.isEnabled()) {
                return;
            }

            DataSource sharedDataSource = flyway.getConfiguration().getDataSource();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(sharedDataSource);

            List<TenantRow> tenants = jdbcTemplate.query(
                    "SELECT id, tenancy_mode, schema_name, db_host, db_name, db_username, db_password " +
                            "FROM tenants WHERE active = true",
                    (rs, rowNum) -> new TenantRow(
                            rs.getLong("id"),
                            rs.getString("tenancy_mode"),
                            rs.getString("schema_name"),
                            rs.getString("db_host"),
                            rs.getString("db_name"),
                            rs.getString("db_username"),
                            rs.getString("db_password")
                    )
            );

            for (TenantRow tenant : tenants) {
                if ("SHARED_SCHEMA".equalsIgnoreCase(tenant.tenancyMode()) && properties.isIncludeSharedSchemas()) {
                    migrateSharedSchemaTenant(sharedDataSource, properties, tenant);
                } else if ("DEDICATED_DB".equalsIgnoreCase(tenant.tenancyMode()) && properties.isIncludeDedicatedDbs()) {
                    migrateDedicatedDbTenant(properties, tenant);
                }
            }
        };
    }

    private void migrateSharedSchemaTenant(DataSource sharedDataSource, TenantFlywayProperties properties, TenantRow tenant) {
        String targetSchema = tenant.schemaName();
        if (targetSchema == null || targetSchema.isBlank()) {
            targetSchema = properties.getDefaultSharedSchema();
        }

        if (properties.getDefaultSharedSchema().equalsIgnoreCase(targetSchema)) {
            return;
        }

        try (Connection connection = sharedDataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS \"" + targetSchema.replace("\"", "\"\"") + "\"");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not prepare schema for tenant " + tenant.id(), ex);
        }

        Flyway.configure()
                .dataSource(sharedDataSource)
                .locations(properties.getLocations().toArray(new String[0]))
                .schemas(targetSchema)
                .defaultSchema(targetSchema)
                .table(properties.getHistoryTable())
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .connectRetries(properties.getConnectRetries())
                .load()
                .migrate();
    }

    private void migrateDedicatedDbTenant(TenantFlywayProperties properties, TenantRow tenant) {
        if (isBlank(tenant.dbHost()) || isBlank(tenant.dbName()) || isBlank(tenant.dbUsername())) {
            return;
        }

        String url;
        if (tenant.dbHost().startsWith("jdbc:")) {
            url = tenant.dbHost();
        } else {
            url = "jdbc:postgresql://" + tenant.dbHost() + "/" + tenant.dbName();
        }

        Flyway.configure()
                .dataSource(url, tenant.dbUsername(), tenant.dbPassword())
                .locations(properties.getLocations().toArray(new String[0]))
                .table(properties.getHistoryTable())
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .connectRetries(properties.getConnectRetries())
                .load()
                .migrate();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TenantRow(
            Long id,
            String tenancyMode,
            String schemaName,
            String dbHost,
            String dbName,
            String dbUsername,
            String dbPassword
    ) {
    }
}
