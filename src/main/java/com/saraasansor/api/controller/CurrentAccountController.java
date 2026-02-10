package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CurrentAccountDto;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.service.CurrentAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/current-accounts")
public class CurrentAccountController {
    
    @Autowired
    private CurrentAccountService currentAccountService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<CurrentAccountDto>>> getAllCurrentAccounts() {
        try {
            List<CurrentAccountDto> accounts = currentAccountService.getAllCurrentAccounts().stream()
                    .map(CurrentAccountDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(accounts));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrentAccountDto>> getCurrentAccountById(@PathVariable Long id) {
        try {
            CurrentAccount account = currentAccountService.getCurrentAccountById(id);
            return ResponseEntity.ok(ApiResponse.success(CurrentAccountDto.fromEntity(account)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/building/{buildingId}")
    public ResponseEntity<ApiResponse<CurrentAccountDto>> getByBuildingId(@PathVariable Long buildingId) {
        try {
            Optional<CurrentAccount> accountOpt = currentAccountService.getByBuildingId(buildingId);
            if (accountOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(CurrentAccountDto.fromEntity(accountOpt.get())));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Current account not found for building"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CurrentAccountDto>> updateCurrentAccount(
            @PathVariable Long id, @RequestBody CurrentAccountDto dto) {
        try {
            CurrentAccount account = currentAccountService.getCurrentAccountById(id);
            account.setName(dto.getName());
            account.setAuthorizedPerson(dto.getAuthorizedPerson());
            account.setPhone(dto.getPhone());
            if (dto.getDebt() != null) {
                account.setDebt(dto.getDebt());
            }
            if (dto.getCredit() != null) {
                account.setCredit(dto.getCredit());
            }
            
            CurrentAccount updated = currentAccountService.updateCurrentAccount(id, account);
            return ResponseEntity.ok(ApiResponse.success("Current account successfully updated", CurrentAccountDto.fromEntity(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PatchMapping("/{id}/balance")
    public ResponseEntity<ApiResponse<CurrentAccountDto>> updateBalance(
            @PathVariable Long id, @RequestBody BalanceUpdateRequest request) {
        try {
            CurrentAccount updated = currentAccountService.updateBalance(
                id,
                request.getDeltaDebt(),
                request.getDeltaCredit()
            );
            return ResponseEntity.ok(ApiResponse.success("Balance successfully updated", CurrentAccountDto.fromEntity(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    public static class BalanceUpdateRequest {
        private BigDecimal deltaDebt;
        private BigDecimal deltaCredit;
        
        public BigDecimal getDeltaDebt() {
            return deltaDebt;
        }
        
        public void setDeltaDebt(BigDecimal deltaDebt) {
            this.deltaDebt = deltaDebt;
        }
        
        public BigDecimal getDeltaCredit() {
            return deltaCredit;
        }
        
        public void setDeltaCredit(BigDecimal deltaCredit) {
            this.deltaCredit = deltaCredit;
        }
    }
}
