package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Runtime'da aktif tenant'a gore uygun DataSource'u sececek routing katmani.
 * Simdilik tum tenantlar ortak SHARED DataSource'u kullanir; DEDICATED_DB
 * destegi bir sonraki adimda DynamicDataSourceRegistry ile eklenecektir.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    public static final String SHARED_KEY = "SHARED";

    @Override
    protected Object determineCurrentLookupKey() {
        TenantDescriptor tenant = TenantContext.getCurrentTenant();

        if (tenant == null) {
            // Subdomain yoksa veya tenant cozumlenemediyse mevcut tek-tenant davranisi
            return SHARED_KEY;
        }

        if (tenant.getTenancyMode() == Tenant.TenancyMode.SHARED_SCHEMA) {
            return SHARED_KEY;
        }

        // DEDICATED_DB tenant'lar icin benzersiz key (ileride DynamicDataSourceRegistry ile desteklenecek)
        return "TENANT_DB_" + tenant.getId();
    }
}

