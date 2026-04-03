package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CollectionReceiptPageResponse;
import com.saraasansor.api.dto.CollectionReceiptPrintResponse;
import com.saraasansor.api.service.B2BUnitTransactionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/collection-receipts")
public class CollectionReceiptController {

    private final B2BUnitTransactionService transactionService;

    public CollectionReceiptController(B2BUnitTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER','CARI_USER')")
    public ResponseEntity<ApiResponse<CollectionReceiptPageResponse>> getCollectionReceipts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        CollectionReceiptPageResponse response = transactionService.getCollectionReceipts(
                startDate,
                endDate,
                search,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/print")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER','CARI_USER')")
    public ResponseEntity<ApiResponse<CollectionReceiptPrintResponse>> getCollectionReceiptPrint(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getCollectionReceiptPrint(id)));
    }
}
