package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.VatRateCreateRequest;
import com.saraasansor.api.dto.VatRateLookupDto;
import com.saraasansor.api.dto.VatRatePageResponse;
import com.saraasansor.api.dto.VatRateResponse;
import com.saraasansor.api.service.VatRateService;
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

import java.math.BigDecimal;
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

class VatRateControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void listEndpointShouldWorkWithResolvedTenantContext() throws Exception {
        VatRateService vatRateService = mock(VatRateService.class);
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

        VatRatePageResponse response = new VatRatePageResponse();
        response.setPage(0);
        response.setSize(25);
        response.setTotalElements(1);
        response.setTotalPages(1);
        com.saraasansor.api.dto.VatRateListItemResponse item = new com.saraasansor.api.dto.VatRateListItemResponse();
        item.setId(1L);
        item.setRate(new BigDecimal("20.00"));
        item.setActive(true);
        response.setContent(List.of(item));
        when(vatRateService.getVatRates(any(), any(), any())).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new VatRateController(vatRateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/vat-rates")
                        .with(host("acme.example.com"))
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].rate").value(20.00));

        verify(vatRateService, times(1)).getVatRates(any(), any(), any());
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void createEndpointShouldReturnCreatedVatRate() throws Exception {
        VatRateService vatRateService = mock(VatRateService.class);
        VatRateResponse response = new VatRateResponse();
        response.setId(11L);
        response.setRate(new BigDecimal("10.00"));
        response.setActive(true);
        when(vatRateService.createVatRate(any(VatRateCreateRequest.class))).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new VatRateController(vatRateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("10.00", true));
        mockMvc.perform(post("/vat-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(11));
    }

    @Test
    void lookupEndpointShouldReturnLookupRows() throws Exception {
        VatRateService vatRateService = mock(VatRateService.class);
        VatRateLookupDto dto = new VatRateLookupDto();
        dto.setId(1L);
        dto.setRate(new BigDecimal("20.00"));
        when(vatRateService.getLookup("2")).thenReturn(List.of(dto));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new VatRateController(vatRateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/vat-rates/lookup").param("query", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].rate").value(20.00));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String rate, Boolean active) {
    }
}
