package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.EInvoiceQueryResponseDto;
import com.saraasansor.api.service.EInvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/einvoice")
public class EInvoiceController {

    private final EInvoiceService eInvoiceService;

    public EInvoiceController(EInvoiceService eInvoiceService) {
        this.eInvoiceService = eInvoiceService;
    }

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<EInvoiceQueryResponseDto>> query(@RequestParam String taxNumber) {
        return ResponseEntity.ok(ApiResponse.success(eInvoiceService.queryByTaxNumber(taxNumber)));
    }
}
