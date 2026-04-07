package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.VatRateCreateRequest;
import com.saraasansor.api.dto.VatRateLookupDto;
import com.saraasansor.api.dto.VatRatePageResponse;
import com.saraasansor.api.dto.VatRateResponse;
import com.saraasansor.api.dto.VatRateUpdateRequest;
import com.saraasansor.api.service.VatRateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/vat-rates")
public class VatRateController {

    private final VatRateService vatRateService;

    public VatRateController(VatRateService vatRateService) {
        this.vatRateService = vatRateService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<VatRatePageResponse>> getVatRates(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                vatRateService.getVatRates(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<VatRateResponse>> getVatRateById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vatRateService.getVatRateById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<VatRateResponse>> createVatRate(@Valid @RequestBody VatRateCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("VAT rate created successfully", vatRateService.createVatRate(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<VatRateResponse>> updateVatRate(@PathVariable Long id,
                                                                       @Valid @RequestBody VatRateUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("VAT rate updated successfully", vatRateService.updateVatRate(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteVatRate(@PathVariable Long id) {
        vatRateService.deleteVatRate(id);
        return ResponseEntity.ok(ApiResponse.success("VAT rate deactivated successfully", null));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<List<VatRateLookupDto>>> getLookup(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(vatRateService.getLookup(query)));
    }
}
