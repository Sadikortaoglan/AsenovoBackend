package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitDetailResponse;
import com.saraasansor.api.service.B2BUnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitDetailController {

    private final B2BUnitService b2bUnitService;

    public B2BUnitDetailController(B2BUnitService b2bUnitService) {
        this.b2bUnitService = b2bUnitService;
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<B2BUnitDetailResponse>> getB2BUnitDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(b2bUnitService.getB2BUnitDetail(id)));
    }
}
