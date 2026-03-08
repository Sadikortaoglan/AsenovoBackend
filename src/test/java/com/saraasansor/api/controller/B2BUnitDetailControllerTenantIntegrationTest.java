package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitDetailResponse;
import com.saraasansor.api.service.B2BUnitService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitDetailControllerTenantIntegrationTest {

    @Test
    void detailEndpointShouldReturnMenuWithResolvedTenantContext() throws Exception {
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

        B2BUnitDetailResponse detail = new B2BUnitDetailResponse();
        detail.setId(10L);
        detail.setName("Acme Unit");
        detail.setMenus(List.of(
                new B2BUnitDetailResponse.MenuItem("filter", "Filter"),
                new B2BUnitDetailResponse.MenuItem("invoice", "Invoice")
        ));
        detail.setSummary(new B2BUnitDetailResponse.Summary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(b2bUnitService.getB2BUnitDetail(10L)).thenReturn(detail);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitDetailController(b2bUnitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/10/detail").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.menus[0].key").value("filter"))
                .andExpect(jsonPath("$.data.menus[1].key").value("invoice"));

        verify(b2bUnitService, times(1)).getB2BUnitDetail(10L);
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
