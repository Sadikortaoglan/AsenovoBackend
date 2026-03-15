package com.saraasansor.api.config;

import com.saraasansor.api.tenant.DedicatedTenantDataSourceRegistry;
import com.saraasansor.api.tenant.TenantRoutingDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
@Configuration
public class TenantDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties sharedDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource sharedDataSource(@Qualifier("sharedDataSourceProperties") DataSourceProperties sharedDataSourceProperties) {
        return sharedDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSource sharedDataSource, DedicatedTenantDataSourceRegistry dedicatedTenantDataSourceRegistry) {
        return new TenantRoutingDataSource(sharedDataSource, dedicatedTenantDataSourceRegistry);
    }
}
