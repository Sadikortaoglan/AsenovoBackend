package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitLookupDto;
import com.saraasansor.api.service.B2BUnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitLookupAliasController {

    private final B2BUnitService b2bUnitService;

    public B2BUnitLookupAliasController(B2BUnitService b2bUnitService) {
        this.b2bUnitService = b2bUnitService;
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<List<B2BUnitLookupDto>>> lookupB2BUnits(
            @RequestParam(required = false) String query) {
        List<B2BUnitLookupDto> result = b2bUnitService.getLookup(query).stream()
                .map(B2BUnitLookupDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
