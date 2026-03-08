package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.dto.B2BUnitTransactionResponse;
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
import java.util.List;
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

class B2BUnitTransactionControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void manualDebitEndpointShouldUseResolvedTenantContext() throws Exception {
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

        B2BUnitTransactionResponse tx = new B2BUnitTransactionResponse();
        tx.setId(10L);
        tx.setB2bUnitId(5L);
        tx.setTransactionDate(LocalDate.of(2026, 3, 9));
        tx.setTransactionType(B2BUnitTransaction.TransactionType.MANUAL_DEBIT);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setDebitAmount(new BigDecimal("50.00"));
        tx.setCreditAmount(BigDecimal.ZERO);
        tx.setBalanceAfterTransaction(new BigDecimal("150.00"));
        tx.setStatus(B2BUnitTransaction.TransactionStatus.POSTED);
        tx.setCreatedAt(LocalDateTime.of(2026, 3, 9, 10, 0));

        when(transactionService.createManualDebit(eq(5L), any())).thenReturn(tx);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitTransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new ManualPayload(
                "2026-03-09",
                101L,
                "50.00",
                "manual debit"
        ));

        mockMvc.perform(post("/b2b-units/5/account-transactions/manual-debit")
                        .with(host("acme.example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.transactionType").value("MANUAL_DEBIT"));

        verify(transactionService, times(1)).createManualDebit(eq(5L), any());
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void manualCreditEndpointShouldReturnValidationErrorForMissingAmount() throws Exception {
        B2BUnitTransactionService transactionService = mock(B2BUnitTransactionService.class);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitTransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = """
                {
                  "transactionDate": "2026-03-09",
                  "description": "missing amount"
                }
                """;

        mockMvc.perform(post("/b2b-units/5/account-transactions/manual-credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void manualDebitEndpointShouldReturnForbiddenForUnauthorizedRole() throws Exception {
        B2BUnitTransactionService transactionService = mock(B2BUnitTransactionService.class);
        when(transactionService.createManualDebit(eq(5L), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Forbidden"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitTransactionController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new ManualPayload(
                "2026-03-09",
                null,
                "50.00",
                "manual debit"
        ));

        mockMvc.perform(post("/b2b-units/5/account-transactions/manual-debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void transactionsListShouldContainManualTransactionShape() {
        B2BUnitTransactionPageResponse pageResponse = new B2BUnitTransactionPageResponse();

        B2BUnitTransactionResponse tx = new B2BUnitTransactionResponse();
        tx.setId(99L);
        tx.setTransactionType(B2BUnitTransaction.TransactionType.MANUAL_CREDIT);
        tx.setDebitAmount(BigDecimal.ZERO);
        tx.setCreditAmount(new BigDecimal("20.00"));
        tx.setBalanceAfterTransaction(new BigDecimal("80.00"));
        tx.setDescription("manual credit");

        pageResponse.setContent(List.of(tx));

        assertThat(pageResponse.getContent()).hasSize(1);
        assertThat(pageResponse.getContent().get(0).getTransactionType())
                .isEqualTo(B2BUnitTransaction.TransactionType.MANUAL_CREDIT);
        assertThat(pageResponse.getContent().get(0).getCreditAmount()).isEqualByComparingTo("20.00");
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record ManualPayload(String transactionDate,
                                 Long facilityId,
                                 String amount,
                                 String description) {
    }
}
