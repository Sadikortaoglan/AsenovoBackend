package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.InvoiceResponse;
import com.saraasansor.api.dto.PurchaseInvoiceCreateRequest;
import com.saraasansor.api.dto.SalesInvoiceCreateRequest;
import com.saraasansor.api.service.B2BUnitInvoiceService;
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
public class B2BUnitInvoiceController {

    private final B2BUnitInvoiceService invoiceService;

    public B2BUnitInvoiceController(B2BUnitInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/{id}/invoices/purchase")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createPurchaseInvoice(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseInvoiceCreateRequest request) {
        InvoiceResponse response = invoiceService.createPurchaseInvoice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Purchase invoice created successfully", response));
    }

    @PostMapping("/{id}/invoices/sales")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createSalesInvoice(
            @PathVariable Long id,
            @Valid @RequestBody SalesInvoiceCreateRequest request) {
        InvoiceResponse response = invoiceService.createSalesInvoice(id, request);
        return ResponseEntity.ok(ApiResponse.success("Sales invoice created successfully", response));
    }

    @GetMapping("/{id}/invoices/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable Long id,
            @PathVariable Long invoiceId) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getInvoice(id, invoiceId)));
    }
}
