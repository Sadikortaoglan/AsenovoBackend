package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.BankCollectionCreateRequest;
import com.saraasansor.api.dto.CashCollectionCreateRequest;
import com.saraasansor.api.dto.CheckCollectionCreateRequest;
import com.saraasansor.api.dto.CreditCardCollectionCreateRequest;
import com.saraasansor.api.dto.PaytrCollectionCreateRequest;
import com.saraasansor.api.dto.PromissoryNoteCollectionCreateRequest;
import com.saraasansor.api.service.B2BUnitTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitCollectionController {

    private final B2BUnitTransactionService transactionService;

    public B2BUnitCollectionController(B2BUnitTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{id}/collections/cash")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createCashCollection(
            @PathVariable Long id,
            @Valid @RequestBody CashCollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cash collection transaction created successfully",
                transactionService.createCashCollection(id, request)
        ));
    }

    @PostMapping("/{id}/collections/paytr")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createPaytrCollection(
            @PathVariable Long id,
            @Valid @RequestBody PaytrCollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "PayTR collection transaction created successfully",
                transactionService.createPaytrCollection(id, request)
        ));
    }

    @PostMapping("/{id}/collections/credit-card")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createCreditCardCollection(
            @PathVariable Long id,
            @Valid @RequestBody CreditCardCollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Credit card collection transaction created successfully",
                transactionService.createCreditCardCollection(id, request)
        ));
    }

    @PostMapping("/{id}/collections/bank")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createBankCollection(
            @PathVariable Long id,
            @Valid @RequestBody BankCollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Bank collection transaction created successfully",
                transactionService.createBankCollection(id, request)
        ));
    }

    @PostMapping("/{id}/collections/check")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createCheckCollection(
            @PathVariable Long id,
            @Valid @RequestBody CheckCollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check collection transaction created successfully",
                transactionService.createCheckCollection(id, request)
        ));
    }

    @PostMapping("/{id}/collections/promissory-note")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createPromissoryNoteCollection(
            @PathVariable Long id,
            @Valid @RequestBody PromissoryNoteCollectionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Promissory note collection transaction created successfully",
                transactionService.createPromissoryNoteCollection(id, request)
        ));
    }

    @GetMapping("/{id}/collections/{transactionId}")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> getCollectionById(
            @PathVariable Long id,
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getCollectionById(id, transactionId)));
    }
}
