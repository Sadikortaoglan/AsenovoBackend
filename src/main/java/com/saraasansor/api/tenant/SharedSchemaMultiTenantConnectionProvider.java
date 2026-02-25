package com.saraasansor.api.tenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Tum tenant'lar icin ayni DataSource'u kullanir, fakat PostgreSQL schema'sini
 * tenant identifier'a gore degistirir. Shared-schema tenant'lar icin kullanilacak.
 */
public class SharedSchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider {

    private final DataSource dataSource;
    private final String defaultSchema;

    public SharedSchemaMultiTenantConnectionProvider(DataSource dataSource, String defaultSchema) {
        this.dataSource = dataSource;
        this.defaultSchema = defaultSchema;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        try {
            if (tenantIdentifier != null && !tenantIdentifier.isBlank()) {
                connection.setSchema(tenantIdentifier);
            } else {
                connection.setSchema(defaultSchema);
            }
        } catch (SQLException ex) {
            // Schema yoksa veya hataliysa fallback olarak default schema'ya geri don
            connection.setSchema(defaultSchema);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            connection.setSchema(defaultSchema);
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return unwrapType.isAssignableFrom(getClass()) || unwrapType.isAssignableFrom(dataSource.getClass());
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (unwrapType.isAssignableFrom(getClass())) {
            return unwrapType.cast(this);
        }
        if (unwrapType.isAssignableFrom(dataSource.getClass())) {
            return unwrapType.cast(dataSource);
        }
        throw new IllegalArgumentException("Unknown unwrap type: " + unwrapType);
    }
}

