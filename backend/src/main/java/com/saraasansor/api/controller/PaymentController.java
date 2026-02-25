package com.saraasansor.api.controller;

import com.saraasansor.api.dto.*;
import com.saraasansor.api.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/payment-transactions")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentTransactionDto>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.list(from, to, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> create(@Valid @RequestBody PaymentTransactionDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Payment created", paymentService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentTransactionDto>> update(@PathVariable Long id, @Valid @RequestBody PaymentTransactionDto dto) {
        return ResponseEntity.ok(ApiResponse.success("Payment updated", paymentService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted", null));
    }

    @GetMapping("/cash-accounts")
    public ResponseEntity<ApiResponse<List<CashAccountDto>>> listCashAccounts() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.listCashAccounts()));
    }

    @PostMapping("/cash-accounts")
    public ResponseEntity<ApiResponse<CashAccountDto>> saveCashAccount(@Valid @RequestBody CashAccountDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Cash account saved", paymentService.saveCashAccount(dto)));
    }

    @DeleteMapping("/cash-accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCashAccount(@PathVariable Long id) {
        paymentService.deleteCashAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Cash account deleted", null));
    }

    @GetMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<List<BankAccountDto>>> listBankAccounts() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.listBankAccounts()));
    }

    @PostMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<BankAccountDto>> saveBankAccount(@Valid @RequestBody BankAccountDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Bank account saved", paymentService.saveBankAccount(dto)));
    }

    @DeleteMapping("/bank-accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBankAccount(@PathVariable Long id) {
        paymentService.deleteBankAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Bank account deleted", null));
    }
}
