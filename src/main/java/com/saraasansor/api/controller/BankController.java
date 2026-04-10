package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.BankCreateRequest;
import com.saraasansor.api.dto.BankLookupDto;
import com.saraasansor.api.dto.BankPageResponse;
import com.saraasansor.api.dto.BankResponse;
import com.saraasansor.api.dto.BankUpdateRequest;
import com.saraasansor.api.service.BankAccountService;
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
@RequestMapping("/banks")
public class BankController {

    private final BankAccountService bankAccountService;

    public BankController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<BankPageResponse>> getBanks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bankAccountService.getBanks(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<BankResponse>> getBankById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getBankById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<BankResponse>> createBank(@Valid @RequestBody BankCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bank created successfully", bankAccountService.createBank(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<BankResponse>> updateBank(@PathVariable Long id,
                                                                 @Valid @RequestBody BankUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bank updated successfully", bankAccountService.updateBank(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteBank(@PathVariable Long id) {
        bankAccountService.deleteBank(id);
        return ResponseEntity.ok(ApiResponse.success("Bank deactivated successfully", null));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<List<BankLookupDto>>> getLookup(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(bankAccountService.getBankLookup(query)));
    }
}
