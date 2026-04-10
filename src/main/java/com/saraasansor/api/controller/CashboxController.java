package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CashboxCreateRequest;
import com.saraasansor.api.dto.CashboxLookupDto;
import com.saraasansor.api.dto.CashboxPageResponse;
import com.saraasansor.api.dto.CashboxResponse;
import com.saraasansor.api.dto.CashboxUpdateRequest;
import com.saraasansor.api.service.CashAccountService;
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
@RequestMapping("/cashboxes")
public class CashboxController {

    private final CashAccountService cashAccountService;

    public CashboxController(CashAccountService cashAccountService) {
        this.cashAccountService = cashAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<CashboxPageResponse>> getCashboxes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                cashAccountService.getCashboxes(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<CashboxResponse>> getCashbox(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(cashAccountService.getCashboxById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<CashboxResponse>> createCashbox(@Valid @RequestBody CashboxCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cashbox created successfully", cashAccountService.createCashbox(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<CashboxResponse>> updateCashbox(@PathVariable Long id,
                                                                       @Valid @RequestBody CashboxUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cashbox updated successfully", cashAccountService.updateCashbox(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteCashbox(@PathVariable Long id) {
        cashAccountService.deleteCashbox(id);
        return ResponseEntity.ok(ApiResponse.success("Cashbox deactivated successfully", null));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<List<CashboxLookupDto>>> getCashboxLookup(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(cashAccountService.getCashboxLookup(query)));
    }
}
