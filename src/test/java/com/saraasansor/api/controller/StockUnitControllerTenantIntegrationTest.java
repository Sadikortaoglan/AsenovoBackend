package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.StockUnitCreateRequest;
import com.saraasansor.api.dto.StockUnitLookupDto;
import com.saraasansor.api.dto.StockUnitPageResponse;
import com.saraasansor.api.dto.StockUnitResponse;
import com.saraasansor.api.service.StockUnitService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockUnitControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void stockUnitLookupShouldWorkWithResolvedTenantContext() throws Exception {
        StockUnitService stockUnitService = mock(StockUnitService.class);
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

        StockUnitLookupDto lookup = new StockUnitLookupDto();
        lookup.setId(2L);
        lookup.setName("Adet");
        lookup.setAbbreviation("AD");
        when(stockUnitService.getLookup("ad")).thenReturn(List.of(lookup));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockUnitController(stockUnitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/stock-units/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "ad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Adet"))
                .andExpect(jsonPath("$.data[0].abbreviation").value("AD"));

        verify(stockUnitService, times(1)).getLookup("ad");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void stockUnitCrudShouldReturnExpectedPayload() throws Exception {
        StockUnitService stockUnitService = mock(StockUnitService.class);
        StockUnitResponse response = new StockUnitResponse();
        response.setId(10L);
        response.setName("Metre");
        response.setAbbreviation("M");
        response.setActive(true);
        when(stockUnitService.createStockUnit(any(StockUnitCreateRequest.class))).thenReturn(response);
        when(stockUnitService.getStockUnits(any(), any(), any())).thenReturn(new StockUnitPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockUnitController(stockUnitService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/stock-units")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("Metre", "M", true));
        mockMvc.perform(post("/stock-units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.abbreviation").value("M"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String name, String abbreviation, Boolean active) {
    }
}
