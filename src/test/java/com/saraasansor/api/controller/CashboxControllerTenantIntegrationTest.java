package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.CashboxCreateRequest;
import com.saraasansor.api.dto.CashboxLookupDto;
import com.saraasansor.api.dto.CashboxPageResponse;
import com.saraasansor.api.dto.CashboxResponse;
import com.saraasansor.api.service.CashAccountService;
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

class CashboxControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void cashboxLookupShouldWorkWithResolvedTenantContext() throws Exception {
        CashAccountService cashAccountService = mock(CashAccountService.class);
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

        CashboxLookupDto lookup = new CashboxLookupDto();
        lookup.setId(10L);
        lookup.setName("Main Cashbox");
        lookup.setCurrencyCode("TRY");
        lookup.setCurrencyName("Turkish Lira");
        when(cashAccountService.getCashboxLookup("main")).thenReturn(List.of(lookup));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new CashboxController(cashAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/cashboxes/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Main Cashbox"));

        verify(cashAccountService, times(1)).getCashboxLookup("main");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void cashboxCreateShouldReturnCreatedCashbox() throws Exception {
        CashAccountService cashAccountService = mock(CashAccountService.class);
        CashboxResponse response = new CashboxResponse();
        response.setId(21L);
        response.setName("Front Desk");
        response.setCurrencyCode("USD");
        when(cashAccountService.createCashbox(any(CashboxCreateRequest.class))).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new CashboxController(cashAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("Front Desk", "USD", true, "desc"));
        mockMvc.perform(post("/cashboxes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(21));
    }

    @Test
    void cashboxListShouldReturnPagedPayload() throws Exception {
        CashAccountService cashAccountService = mock(CashAccountService.class);
        CashboxPageResponse page = new CashboxPageResponse();
        page.setPage(0);
        page.setSize(25);
        page.setTotalElements(0);
        page.setTotalPages(0);
        when(cashAccountService.getCashboxes(any(), any(), any())).thenReturn(page);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new CashboxController(cashAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/cashboxes")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String name, String currencyCode, Boolean active, String description) {
    }
}
