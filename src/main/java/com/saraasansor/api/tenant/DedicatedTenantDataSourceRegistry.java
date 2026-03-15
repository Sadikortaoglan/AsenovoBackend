package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Demo gibi DEDICATED_DB tenant'lar icin DataSource havuzlarini lazy olusturur.
 * Shared tenant akisini degistirmez; yalnizca dedicated tenant geldiginde devreye girer.
 */
@Component
public class DedicatedTenantDataSourceRegistry {

    private final ConcurrentMap<Long, DataSource> cache = new ConcurrentHashMap<>();
    private final DataSourceProperties sharedDataSourceProperties;

    public DedicatedTenantDataSourceRegistry(@Qualifier("sharedDataSourceProperties") DataSourceProperties sharedDataSourceProperties) {
        this.sharedDataSourceProperties = sharedDataSourceProperties;
    }

    public DataSource resolve(TenantDescriptor tenant) {
        Objects.requireNonNull(tenant, "tenant");
        return cache.computeIfAbsent(tenant.getId(), ignored -> createDataSource(tenant));
    }

    public void evict(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        DataSource dataSource = cache.remove(tenantId);
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.close();
        }
    }

    private DataSource createDataSource(TenantDescriptor tenant) {
        String jdbcUrl = resolveJdbcUrl(tenant);
        String username = firstNonBlank(tenant.getDbUsername(), sharedDataSourceProperties.getUsername());
        String password = firstNonBlank(tenant.getDbPassword(), sharedDataSourceProperties.getPassword());

        HikariConfig config = new HikariConfig();
        config.setPoolName("tenant-db-" + tenant.getId());
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(firstNonBlank(sharedDataSourceProperties.getDriverClassName(), "org.postgresql.Driver"));
        config.setMaximumPoolSize(4);
        config.setMinimumIdle(0);
        config.setIdleTimeout(60_000);
        config.setConnectionTimeout(5_000);
        config.setValidationTimeout(3_000);
        config.setLeakDetectionThreshold(0);
        config.addDataSourceProperty("reWriteBatchedInserts", "true");
        config.addDataSourceProperty("ApplicationName", "asenovo-dedicated-tenant");
        return new HikariDataSource(config);
    }

    private String resolveJdbcUrl(TenantDescriptor tenant) {
        if (tenant.getDbHost() == null || tenant.getDbHost().isBlank()) {
            throw new IllegalStateException("Dedicated tenant is missing dbHost: " + tenant.getSubdomain());
        }
        if (tenant.getDbHost().startsWith("jdbc:")) {
            return tenant.getDbHost();
        }
        if (tenant.getDbName() == null || tenant.getDbName().isBlank()) {
            throw new IllegalStateException("Dedicated tenant is missing dbName: " + tenant.getSubdomain());
        }
        return "jdbc:postgresql://" + tenant.getDbHost() + "/" + tenant.getDbName();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }
}
