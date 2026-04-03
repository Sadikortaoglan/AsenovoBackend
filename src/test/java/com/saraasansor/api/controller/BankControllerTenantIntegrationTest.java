package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.BankCreateRequest;
import com.saraasansor.api.dto.BankLookupDto;
import com.saraasansor.api.dto.BankPageResponse;
import com.saraasansor.api.dto.BankResponse;
import com.saraasansor.api.service.BankAccountService;
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

class BankControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void bankLookupShouldUseResolvedTenantContext() throws Exception {
        BankAccountService bankAccountService = mock(BankAccountService.class);
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

        BankLookupDto dto = new BankLookupDto();
        dto.setId(1L);
        dto.setName("Akbank");
        dto.setBranchName("Kadikoy");
        dto.setCurrencyCode("TRY");
        when(bankAccountService.getBankLookup("ak")).thenReturn(List.of(dto));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new BankController(bankAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/banks/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "ak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Akbank"));

        verify(bankAccountService, times(1)).getBankLookup("ak");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void bankCrudEndpointsShouldReturnSuccessPayload() throws Exception {
        BankAccountService bankAccountService = mock(BankAccountService.class);
        BankResponse response = new BankResponse();
        response.setId(10L);
        response.setName("Garanti");
        response.setCurrencyCode("USD");
        when(bankAccountService.createBank(any(BankCreateRequest.class))).thenReturn(response);
        when(bankAccountService.getBanks(any(), any(), any())).thenReturn(new BankPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new BankController(bankAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/banks")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("Garanti", "Levent", "9988", "TR220001100000000000000001", "USD", true));
        mockMvc.perform(post("/banks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String name,
                                 String branchName,
                                 String accountNumber,
                                 String iban,
                                 String currencyCode,
                                 Boolean active) {
    }
}
