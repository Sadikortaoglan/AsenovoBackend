package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.PaymentReceiptDto;
import com.saraasansor.api.dto.PaymentSummaryDto;
import com.saraasansor.api.service.PaymentReceiptService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentReceiptController {
    
    @Autowired
    private PaymentReceiptService paymentReceiptService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentReceiptDto>>> getPaymentReceipts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        List<PaymentReceiptDto> receipts = paymentReceiptService.getPaymentReceiptsByDateRange(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success(receipts));
    }
    
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PaymentSummaryDto>> getSummary() {
        PaymentSummaryDto summary = paymentReceiptService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentReceiptDto>> getPaymentReceiptById(@PathVariable Long id) {
        try {
            PaymentReceiptDto receipt = paymentReceiptService.getPaymentReceiptById(id);
            return ResponseEntity.ok(ApiResponse.success(receipt));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentReceiptDto>> createPaymentReceipt(
            @Valid @RequestBody PaymentReceiptDto dto) {
        try {
            PaymentReceiptDto created = paymentReceiptService.createPaymentReceipt(dto);
            return ResponseEntity.ok(ApiResponse.success("Payment receipt successfully added", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentReceiptDto>> updatePaymentReceipt(
            @PathVariable Long id, @Valid @RequestBody PaymentReceiptDto dto) {
        try {
            PaymentReceiptDto updated = paymentReceiptService.updatePaymentReceipt(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Payment receipt successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaymentReceipt(@PathVariable Long id) {
        try {
            paymentReceiptService.deletePaymentReceipt(id);
            return ResponseEntity.ok(ApiResponse.success("Payment receipt successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

