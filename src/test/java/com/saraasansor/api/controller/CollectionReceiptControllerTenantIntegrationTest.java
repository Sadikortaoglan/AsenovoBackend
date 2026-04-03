package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.CollectionReceiptListItemResponse;
import com.saraasansor.api.dto.CollectionReceiptPageResponse;
import com.saraasansor.api.dto.CollectionReceiptPrintResponse;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.service.B2BUnitTransactionService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

class CollectionReceiptControllerTenantIntegrationTest {

    @Test
    void shouldListCollectionReceiptsWithResolvedTenantContext() throws Exception {
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

        CollectionReceiptListItemResponse item = new CollectionReceiptListItemResponse();
        item.setId(77L);
        item.setReceiptNumber("COL-00000077");
        item.setCollectionDate(LocalDate.of(2026, 4, 1));
        item.setCollectionType(B2BUnitTransaction.TransactionType.PAYTR_COLLECTION);
        item.setB2bUnitName("Acme Cari");
        item.setAmount(new BigDecimal("230.00"));
        item.setCreatedAt(LocalDateTime.of(2026, 4, 1, 11, 0));

        CollectionReceiptPageResponse response = new CollectionReceiptPageResponse();
        response.setContent(List.of(item));
        response.setPage(0);
        response.setSize(25);
        response.setTotalElements(1L);
        response.setTotalPages(1);

        when(transactionService.getCollectionReceipts(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                "acme",
                org.springframework.data.domain.PageRequest.of(0, 25)
        )).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new CollectionReceiptController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/collection-receipts")
                        .with(host("acme.example.com"))
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30")
                        .param("search", "acme")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].receiptNumber").value("COL-00000077"));

        verify(transactionService, times(1)).getCollectionReceipts(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                "acme",
                org.springframework.data.domain.PageRequest.of(0, 25)
        );
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void shouldReturnPrintableCollectionReceipt() throws Exception {
        B2BUnitTransactionService transactionService = mock(B2BUnitTransactionService.class);

        CollectionReceiptPrintResponse response = new CollectionReceiptPrintResponse();
        response.setId(7L);
        response.setReceiptNumber("COL-00000007");
        response.setCollectionDate(LocalDate.of(2026, 4, 1));
        response.setCollectionType(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
        response.setB2bUnitName("Cari X");
        response.setFacilityName("Facility A");
        response.setAmount(new BigDecimal("110.00"));

        when(transactionService.getCollectionReceiptPrint(7L)).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new CollectionReceiptController(transactionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/collection-receipts/7/print"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNumber").value("COL-00000007"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
