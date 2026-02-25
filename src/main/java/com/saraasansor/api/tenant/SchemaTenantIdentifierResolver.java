package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.data.TenantDescriptor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Hibernate icin aktif tenant'in schema adini belirler.
 * Tenant yoksa veya schemaName bos ise default schema kullanilir (ornek: "public").
 */
public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    private final String defaultSchema;

    public SchemaTenantIdentifierResolver(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        TenantDescriptor tenant = TenantContext.getCurrentTenant();

        if (tenant == null) {
            return defaultSchema;
        }

        String schemaName = tenant.getSchemaName();
        if (schemaName == null || schemaName.isBlank()) {
            return defaultSchema;
        }

        return schemaName;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}

