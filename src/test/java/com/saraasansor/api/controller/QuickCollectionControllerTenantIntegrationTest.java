package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.QuickCollectionCreateRequest;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.service.QuickCollectionService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuickCollectionControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void shouldCreateQuickCollectionWithResolvedTenantContext() throws Exception {
        QuickCollectionService quickCollectionService = mock(QuickCollectionService.class);
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

        B2BUnitCollectionTransactionResponse response = new B2BUnitCollectionTransactionResponse();
        response.setId(101L);
        response.setB2bUnitId(6L);
        response.setTransactionDate(LocalDate.of(2026, 4, 1));
        response.setTransactionType(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
        response.setAmount(new BigDecimal("150.00"));
        response.setCreditAmount(new BigDecimal("150.00"));
        response.setBalanceAfterTransaction(new BigDecimal("350.00"));
        response.setCreatedAt(LocalDateTime.of(2026, 4, 1, 14, 0));

        when(quickCollectionService.createCollection(any())).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new QuickCollectionController(quickCollectionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new QuickPayload(
                "2026-04-01",
                6L,
                11L,
                QuickCollectionCreateRequest.CollectionType.CASH,
                "150.00",
                "quick collection",
                10L
        ));

        mockMvc.perform(post("/quick-collections")
                        .with(host("acme.example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionType").value("CASH_COLLECTION"));

        verify(quickCollectionService, times(1)).createCollection(any());
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record QuickPayload(String collectionDate,
                                Long b2bUnitId,
                                Long facilityId,
                                QuickCollectionCreateRequest.CollectionType collectionType,
                                String amount,
                                String description,
                                Long cashboxId) {
    }
}
