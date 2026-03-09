package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.BankPaymentCreateRequest;
import com.saraasansor.api.dto.B2BUnitPaymentTransactionResponse;
import com.saraasansor.api.dto.CashPaymentCreateRequest;
import com.saraasansor.api.dto.CheckPaymentCreateRequest;
import com.saraasansor.api.dto.CreditCardPaymentCreateRequest;
import com.saraasansor.api.dto.PromissoryNotePaymentCreateRequest;
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
public class B2BUnitPaymentController {

    private final B2BUnitTransactionService transactionService;

    public B2BUnitPaymentController(B2BUnitTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/{id}/payments/cash")
    public ResponseEntity<ApiResponse<B2BUnitPaymentTransactionResponse>> createCashPayment(
            @PathVariable Long id,
            @Valid @RequestBody CashPaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cash payment transaction created successfully",
                transactionService.createCashPayment(id, request)
        ));
    }

    @PostMapping("/{id}/payments/credit-card")
    public ResponseEntity<ApiResponse<B2BUnitPaymentTransactionResponse>> createCreditCardPayment(
            @PathVariable Long id,
            @Valid @RequestBody CreditCardPaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Credit card payment transaction created successfully",
                transactionService.createCreditCardPayment(id, request)
        ));
    }

    @PostMapping("/{id}/payments/bank")
    public ResponseEntity<ApiResponse<B2BUnitPaymentTransactionResponse>> createBankPayment(
            @PathVariable Long id,
            @Valid @RequestBody BankPaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Bank payment transaction created successfully",
                transactionService.createBankPayment(id, request)
        ));
    }

    @PostMapping("/{id}/payments/check")
    public ResponseEntity<ApiResponse<B2BUnitPaymentTransactionResponse>> createCheckPayment(
            @PathVariable Long id,
            @Valid @RequestBody CheckPaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Check payment transaction created successfully",
                transactionService.createCheckPayment(id, request)
        ));
    }

    @PostMapping("/{id}/payments/promissory-note")
    public ResponseEntity<ApiResponse<B2BUnitPaymentTransactionResponse>> createPromissoryNotePayment(
            @PathVariable Long id,
            @Valid @RequestBody PromissoryNotePaymentCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Promissory note payment transaction created successfully",
                transactionService.createPromissoryNotePayment(id, request)
        ));
    }

    @GetMapping("/{id}/payments/{transactionId}")
    public ResponseEntity<ApiResponse<B2BUnitPaymentTransactionResponse>> getPaymentById(
            @PathVariable Long id,
            @PathVariable Long transactionId) {
        return ResponseEntity.ok(ApiResponse.success(transactionService.getPaymentById(id, transactionId)));
    }
}
