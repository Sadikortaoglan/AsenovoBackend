package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.service.B2BUnitTransactionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
