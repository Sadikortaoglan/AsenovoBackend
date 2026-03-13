package com.saraasansor.api.tenant.service;

import com.saraasansor.api.config.TenantFlywayProperties;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class TenantMigrationService {

    private final DataSource dataSource;
    private final TenantFlywayProperties tenantFlywayProperties;
    private final SchemaManagementService schemaManagementService;

    public TenantMigrationService(DataSource dataSource,
                                  TenantFlywayProperties tenantFlywayProperties,
                                  SchemaManagementService schemaManagementService) {
        this.dataSource = dataSource;
        this.tenantFlywayProperties = tenantFlywayProperties;
        this.schemaManagementService = schemaManagementService;
    }

    public void migrateSchema(String schemaName) {
        String safeSchemaName = schemaManagementService.sanitizeSchemaName(schemaName);

        Flyway.configure()
                .dataSource(dataSource)
                .locations(tenantFlywayProperties.getLocations().toArray(new String[0]))
                .schemas(safeSchemaName)
                .defaultSchema(safeSchemaName)
                .table(tenantFlywayProperties.getHistoryTable())
                .baselineOnMigrate(tenantFlywayProperties.isBaselineOnMigrate())
                .connectRetries(tenantFlywayProperties.getConnectRetries())
                .createSchemas(false)
                .load()
                .migrate();
    }
}
