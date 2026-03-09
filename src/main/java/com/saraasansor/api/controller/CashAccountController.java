package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.service.CashAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cash-accounts")
public class CashAccountController {

    private final CashAccountService cashAccountService;

    public CashAccountController(CashAccountService cashAccountService) {
        this.cashAccountService = cashAccountService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<List<LookupDto>>> getLookup(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(cashAccountService.getLookup(query)));
    }
}
