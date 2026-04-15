package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.InvoiceLineResponse;
import com.saraasansor.api.dto.InvoiceResponse;
import com.saraasansor.api.model.B2BUnitInvoice;
import com.saraasansor.api.service.B2BUnitInvoiceService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitInvoiceControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void getInvoiceEndpointShouldWorkWithResolvedTenantContext() throws Exception {
        B2BUnitInvoiceService invoiceService = mock(B2BUnitInvoiceService.class);
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

        InvoiceLineResponse line = new InvoiceLineResponse();
        line.setId(1L);
        line.setStockId(2001L);
        line.setStockName("Service A");
        line.setProductName("Service A");
        line.setQuantity(new BigDecimal("1.00"));
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setVatRate(new BigDecimal("20.00"));
        line.setLineSubTotal(new BigDecimal("100.00"));
        line.setLineVatTotal(new BigDecimal("20.00"));
        line.setLineGrandTotal(new BigDecimal("120.00"));

        InvoiceResponse response = new InvoiceResponse();
        response.setId(10L);
        response.setInvoiceType(B2BUnitInvoice.InvoiceType.SALES);
        response.setB2bUnitId(5L);
        response.setInvoiceDate(LocalDate.of(2026, 3, 5));
        response.setSubTotal(new BigDecimal("100.00"));
        response.setVatTotal(new BigDecimal("20.00"));
        response.setGrandTotal(new BigDecimal("120.00"));
        response.setLines(List.of(line));

        when(invoiceService.getInvoice(5L, 10L)).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitInvoiceController(invoiceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/invoices/10").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.lines[0].stockId").value(2001))
                .andExpect(jsonPath("$.data.lines[0].stockName").value("Service A"))
                .andExpect(jsonPath("$.data.lines[0].productName").value("Service A"));

        verify(invoiceService, times(1)).getInvoice(5L, 10L);
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void createPurchaseEndpointShouldUseTenantResolvedContext() throws Exception {
        B2BUnitInvoiceService invoiceService = mock(B2BUnitInvoiceService.class);
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

        InvoiceResponse response = new InvoiceResponse();
        response.setId(99L);
        response.setInvoiceType(B2BUnitInvoice.InvoiceType.PURCHASE);
        response.setB2bUnitId(5L);
        response.setGrandTotal(new BigDecimal("59.00"));
        when(invoiceService.createPurchaseInvoice(eq(5L), any())).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitInvoiceController(invoiceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new PurchasePayload(
                3L,
                "2026-03-05",
                "Test",
                List.of(new LinePayload(1001L, "1", "50", "18"))
        ));

        mockMvc.perform(post("/b2b-units/5/invoices/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(99));

        verify(invoiceService, times(1)).createPurchaseInvoice(eq(5L), any());
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record PurchasePayload(Long warehouseId,
                                   String invoiceDate,
                                   String description,
                                   List<LinePayload> lines) {
    }

    private record LinePayload(Long stockId,
                               String quantity,
                               String unitPrice,
                               String vatRate) {
    }
}
