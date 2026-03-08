package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.service.B2BUnitService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitControllerTenantIntegrationTest {

    @Test
    void endpointShouldWorkWithResolvedTenantContext() throws Exception {
        B2BUnitService b2bUnitService = mock(B2BUnitService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);

        when(tenantRegistryService.findActiveBySubdomain("acme"))
                .thenReturn(Optional.of(new TenantDescriptor(
                        1L,
                        "Acme",
                        "acme",
                        Tenant.TenancyMode.SHARED_SCHEMA,
                        "tenant_acme",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "STARTER"
                )));
        B2BUnit unit = new B2BUnit();
        unit.setName("Acme Unit");
        when(b2bUnitService.getB2BUnits(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unit), PageRequest.of(0, 20), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitController(b2bUnitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2bunits").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(b2bUnitService, times(1)).getB2BUnits(eq(null), any(Pageable.class));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void endpointShouldReturnNotFoundForUnknownTenant() throws Exception {
        B2BUnitService b2bUnitService = mock(B2BUnitService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);

        when(tenantRegistryService.findActiveBySubdomain("unknown")).thenReturn(Optional.empty());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitController(b2bUnitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2bunits").with(host("unknown.example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TENANT_NOT_FOUND"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
