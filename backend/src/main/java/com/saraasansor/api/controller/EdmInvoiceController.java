package com.saraasansor.api.controller;

import com.saraasansor.api.dto.*;
import com.saraasansor.api.service.EdmSettingService;
import com.saraasansor.api.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/edm")
public class EdmInvoiceController {
    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private EdmSettingService edmSettingService;

    @GetMapping("/invoices/incoming")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> listIncoming(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.listIncoming(startDate, endDate, status, pageable)));
    }

    @GetMapping("/invoices/outgoing")
    public ResponseEntity<ApiResponse<Page<InvoiceDto>>> listOutgoing(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.listOutgoing(startDate, endDate, status, pageable)));
    }

    @PostMapping("/invoices/manual")
    public ResponseEntity<ApiResponse<InvoiceDto>> createManual(@Valid @RequestBody InvoiceDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Manual invoice created", invoiceService.createManual(dto)));
    }

    @PostMapping("/invoices/merge")
    public ResponseEntity<ApiResponse<InvoiceDto>> merge(@Valid @RequestBody InvoiceMergeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Invoices merged", invoiceService.mergeInvoices(request.getInvoiceIds())));
    }

    @PostMapping("/invoices/transfer-completed")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> transferCompleted(@RequestBody List<Long> maintenancePlanIds) {
        return ResponseEntity.ok(ApiResponse.success("Completed maintenances transferred", invoiceService.transferCompletedMaintenancesToSales(maintenancePlanIds)));
    }

    @GetMapping("/vkn-tckn/validate")
    public ResponseEntity<ApiResponse<VknValidationResponse>> validate(@RequestParam String value) {
        return ResponseEntity.ok(ApiResponse.success(validateVknTckn(value)));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<EdmSettingDto>> getSettings() {
        return ResponseEntity.ok(ApiResponse.success(edmSettingService.getCurrent()));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<EdmSettingDto>> saveSettings(@Valid @RequestBody EdmSettingDto dto) {
        return ResponseEntity.ok(ApiResponse.success("EDM settings saved", edmSettingService.save(dto)));
    }

    private VknValidationResponse validateVknTckn(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s", "");
        if (!normalized.matches("\\d+")) {
            return new VknValidationResponse(false, "UNKNOWN", "Only digits are allowed");
        }

        if (normalized.length() == 10) {
            return new VknValidationResponse(true, "VKN", "VKN format is valid");
        }

        if (normalized.length() == 11) {
            if (isValidTckn(normalized)) {
                return new VknValidationResponse(true, "TCKN", "TCKN checksum is valid");
            }
            return new VknValidationResponse(false, "TCKN", "Invalid TCKN checksum");
        }

        return new VknValidationResponse(false, "UNKNOWN", "Length must be 10 (VKN) or 11 (TCKN)");
    }

    private boolean isValidTckn(String tckn) {
        if (tckn.charAt(0) == '0') return false;
        int sumOdd = 0;
        int sumEven = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(tckn.charAt(i));
            if (i % 2 == 0) sumOdd += digit;
            else sumEven += digit;
        }
        int digit10 = ((sumOdd * 7) - sumEven) % 10;
        int sumFirst10 = 0;
        for (int i = 0; i < 10; i++) sumFirst10 += Character.getNumericValue(tckn.charAt(i));
        int digit11 = sumFirst10 % 10;
        return digit10 == Character.getNumericValue(tckn.charAt(9))
                && digit11 == Character.getNumericValue(tckn.charAt(10));
    }
}
