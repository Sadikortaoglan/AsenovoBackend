package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.StockTransferCreateRequest;
import com.saraasansor.api.dto.StockTransferPageResponse;
import com.saraasansor.api.dto.StockTransferResponse;
import com.saraasansor.api.dto.StockTransferUpdateRequest;
import com.saraasansor.api.service.StockTransferService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-transfers")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockTransferPageResponse>> getStockTransfers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                stockTransferService.getStockTransfers(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> getStockTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stockTransferService.getStockTransferById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> createStockTransfer(
            @Valid @RequestBody StockTransferCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Stock transfer created successfully",
                stockTransferService.createStockTransfer(request)
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockTransferResponse>> updateStockTransfer(
            @PathVariable Long id,
            @Valid @RequestBody StockTransferUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Stock transfer updated successfully",
                stockTransferService.updateStockTransfer(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteStockTransfer(@PathVariable Long id) {
        stockTransferService.deleteStockTransfer(id);
        return ResponseEntity.ok(ApiResponse.success("Stock transfer deactivated successfully", null));
    }
}
