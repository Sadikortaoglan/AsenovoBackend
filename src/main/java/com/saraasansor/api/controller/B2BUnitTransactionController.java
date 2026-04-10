package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CollectionReceiptPageResponse;
import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.dto.B2BUnitTransactionResponse;
import com.saraasansor.api.dto.ManualCreditCreateRequest;
import com.saraasansor.api.dto.ManualDebitCreateRequest;
import com.saraasansor.api.service.B2BUnitTransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitTransactionController {

    private final B2BUnitTransactionService transactionService;

    public B2BUnitTransactionController(B2BUnitTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<B2BUnitTransactionPageResponse>> getTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {

        B2BUnitTransactionPageResponse response = transactionService.getTransactions(
                id,
                startDate,
                endDate,
                search,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER','CARI_USER')")
    public ResponseEntity<ApiResponse<CollectionReceiptPageResponse>> getCollectionReceiptsCompatibility(
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

    @PostMapping("/{id}/account-transactions/manual-debit")
    public ResponseEntity<ApiResponse<B2BUnitTransactionResponse>> createManualDebit(
            @PathVariable Long id,
            @Valid @RequestBody ManualDebitCreateRequest request) {
        B2BUnitTransactionResponse response = transactionService.createManualDebit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Manual debit transaction created successfully", response));
    }

    @PostMapping("/{id}/account-transactions/manual-credit")
    public ResponseEntity<ApiResponse<B2BUnitTransactionResponse>> createManualCredit(
            @PathVariable Long id,
            @Valid @RequestBody ManualCreditCreateRequest request) {
        B2BUnitTransactionResponse response = transactionService.createManualCredit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Manual credit transaction created successfully", response));
    }

    @GetMapping("/{id}/account-transactions/{transactionId}")
    public ResponseEntity<ApiResponse<B2BUnitTransactionResponse>> getTransactionById(
            @PathVariable Long id,
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionById(id, transactionId)));
    }
}
