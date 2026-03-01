package com.saraasansor.api.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSchemaRoutingIntegrationTest {

    @Test
    void shouldRouteConnectionToTenantSchema() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:tenant_routing_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        initializeSchemas(dataSource);
        SharedSchemaMultiTenantConnectionProvider provider =
                new SharedSchemaMultiTenantConnectionProvider(dataSource, "public");

        Connection tenantConnection = provider.getConnection("tenant_acme");
        String tenantValue;
        try {
            tenantValue = fetchSingleName(tenantConnection);
        } finally {
            provider.releaseConnection("tenant_acme", tenantConnection);
        }

        Connection sharedConnection = provider.getConnection(null);
        String sharedValue;
        try {
            sharedValue = fetchSingleName(sharedConnection);
        } finally {
            provider.releaseConnection("public", sharedConnection);
        }

        assertThat(tenantValue).isEqualTo("Tenant Unit");
        assertThat(sharedValue).isEqualTo("Public Unit");
    }

    private void initializeSchemas(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS tenant_acme");
            statement.execute("CREATE TABLE IF NOT EXISTS public.b2b_units (id BIGINT PRIMARY KEY, name VARCHAR(255))");
            statement.execute("CREATE TABLE IF NOT EXISTS tenant_acme.b2b_units (id BIGINT PRIMARY KEY, name VARCHAR(255))");
            statement.execute("MERGE INTO public.b2b_units KEY (id) VALUES (1, 'Public Unit')");
            statement.execute("MERGE INTO tenant_acme.b2b_units KEY (id) VALUES (1, 'Tenant Unit')");
        }
    }

    private String fetchSingleName(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM b2b_units WHERE id = 1")) {
            resultSet.next();
            return resultSet.getString("name");
        }
    }
}
