package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runtime'da aktif tenant'a gore uygun DataSource'u secer.
 * Shared tenant davranisi ayni kalir; yalniz DEDICATED_DB tenant'lar lazy
 * registry uzerinden ayri havuza yonlenir.
 */
public class TenantRoutingDataSource extends AbstractDataSource {

    private final DataSource sharedDataSource;
    private final DedicatedTenantDataSourceRegistry dedicatedRegistry;

    public TenantRoutingDataSource(DataSource sharedDataSource, DedicatedTenantDataSourceRegistry dedicatedRegistry) {
        this.sharedDataSource = sharedDataSource;
        this.dedicatedRegistry = dedicatedRegistry;
    }

    private DataSource determineCurrentDataSource() {
        TenantDescriptor tenant = TenantContext.getCurrentTenant();

        if (tenant == null || tenant.getTenancyMode() == Tenant.TenancyMode.SHARED_SCHEMA) {
            return sharedDataSource;
        }

        return dedicatedRegistry.resolve(tenant);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return determineCurrentDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return determineCurrentDataSource().getConnection(username, password);
    }
}
