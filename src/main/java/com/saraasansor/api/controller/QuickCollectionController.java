package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.QuickCollectionCreateRequest;
import com.saraasansor.api.service.QuickCollectionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quick-collections")
public class QuickCollectionController {

    private final QuickCollectionService quickCollectionService;

    public QuickCollectionController(QuickCollectionService quickCollectionService) {
        this.quickCollectionService = quickCollectionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<B2BUnitCollectionTransactionResponse>> createQuickCollection(
            @Valid @RequestBody QuickCollectionCreateRequest request) {
        B2BUnitCollectionTransactionResponse response = quickCollectionService.createCollection(request);
        return ResponseEntity.ok(ApiResponse.success("Quick collection transaction created successfully", response));
    }
}
