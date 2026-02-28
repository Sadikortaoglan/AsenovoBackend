package com.saraasansor.api.config;

import com.saraasansor.api.tenant.TenantRoutingDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * Uygulamanin ana DataSource'u olarak TenantRoutingDataSource kullanilir.
     * Simdilik tum istekler, tenant olsa da olmasa da, tek bir SHARED DataSource'a
     * yonlenir; DEDICATED_DB tenant'lar icin ayri havuzlar bir sonraki adimda eklenecektir.
     */
    @Bean
    @Primary
    public DataSource dataSource(DataSource sharedDataSource) {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(TenantRoutingDataSource.SHARED_KEY, sharedDataSource);

        routingDataSource.setDefaultTargetDataSource(sharedDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }
}
