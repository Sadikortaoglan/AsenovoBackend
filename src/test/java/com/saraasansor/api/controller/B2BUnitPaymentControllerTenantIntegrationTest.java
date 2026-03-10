package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitPaymentTransactionResponse;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.service.B2BUnitTransactionService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitPaymentControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void cashPaymentEndpointShouldWorkWithResolvedTenantContext() throws Exception {
        B2BUnitTransactionService transactionService = mock(B2BUnitTransactionService.class);
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

        B2BUnitPaymentTransactionResponse response = new B2BUnitPaymentTransactionResponse();
        response.setId(70L);
        response.setB2bUnitId(5L);
        response.setTransactionDate(LocalDate.of(2026, 3, 9));
        response.setTransactionType(B2BUnitTransaction.TransactionType.CASH_PAYMENT);
        response.setAmount(new BigDecimal("50.00"));
        response.setDebitAmount(new BigDecimal("50.00"));
        response.setCreditAmount(BigDecimal.ZERO);
        response.setBalanceAfterTransaction(new BigDecimal("150.00"));
        response.setStatus(B2BUnitTransaction.TransactionStatus.POSTED);
        response.setCreatedAt(LocalDateTime.of(2026, 3, 9, 12, 0));

        when(transactionService.createCashPayment(eq(5L), any())).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitPaymentController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new CashPayload(
                "2026-03-09",
                null,
                "50.00",
                10L,
                "cash payment"
        ));

        mockMvc.perform(post("/b2b-units/5/payments/cash")
                        .with(host("acme.example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.transactionType").value("CASH_PAYMENT"));

        verify(transactionService, times(1)).createCashPayment(eq(5L), any());
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CashPayload(String transactionDate,
                               Long facilityId,
                               String amount,
                               Long cashAccountId,
                               String description) {
    }
}
