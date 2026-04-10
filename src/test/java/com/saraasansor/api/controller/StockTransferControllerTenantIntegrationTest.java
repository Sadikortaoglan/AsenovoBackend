package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.StockTransferCreateRequest;
import com.saraasansor.api.dto.StockTransferPageResponse;
import com.saraasansor.api.dto.StockTransferResponse;
import com.saraasansor.api.service.StockTransferService;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockTransferControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void stockTransferListShouldWorkWithResolvedTenantContext() throws Exception {
        StockTransferService stockTransferService = mock(StockTransferService.class);
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

        when(stockTransferService.getStockTransfers(any(), any(), any())).thenReturn(new StockTransferPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockTransferController(stockTransferService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/stock-transfers")
                        .with(host("acme.example.com"))
                        .param("query", "motor")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void stockTransferCreateShouldReturnExpectedPayload() throws Exception {
        StockTransferService stockTransferService = mock(StockTransferService.class);
        StockTransferResponse response = new StockTransferResponse();
        response.setId(10L);
        response.setDate(LocalDate.of(2026, 4, 8));
        response.setQuantity(5);
        response.setStockId(1L);
        response.setOutgoingWarehouseId(2L);
        response.setIncomingWarehouseId(3L);
        when(stockTransferService.createStockTransfer(any(StockTransferCreateRequest.class))).thenReturn(response);
        when(stockTransferService.getStockTransfers(any(), any(), any())).thenReturn(new StockTransferPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockTransferController(stockTransferService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload(
                "2026-04-08", 1L, 2L, 3L, 5, "Transfer", true
        ));
        mockMvc.perform(post("/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.stockId").value(1))
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    void stockTransferCreateShouldAcceptFrontendAliasPayload() throws Exception {
        StockTransferService stockTransferService = mock(StockTransferService.class);
        StockTransferResponse response = new StockTransferResponse();
        response.setId(11L);
        response.setDate(LocalDate.of(2026, 4, 1));
        response.setQuantity(5);
        response.setStockId(11L);
        response.setOutgoingWarehouseId(3L);
        response.setIncomingWarehouseId(2L);
        when(stockTransferService.createStockTransfer(any(StockTransferCreateRequest.class))).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockTransferController(stockTransferService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String payload = """
                {
                  "transferDate": "2026-04-01",
                  "productId": 11,
                  "fromWarehouseId": 3,
                  "toWarehouseId": 2,
                  "miktar": 5
                }
                """;

        mockMvc.perform(post("/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.stockId").value(11))
                .andExpect(jsonPath("$.data.outgoingWarehouseId").value(3))
                .andExpect(jsonPath("$.data.incomingWarehouseId").value(2))
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String date,
                                 Long stockId,
                                 Long outgoingWarehouseId,
                                 Long incomingWarehouseId,
                                 Integer quantity,
                                 String description,
                                 Boolean active) {
    }
}
