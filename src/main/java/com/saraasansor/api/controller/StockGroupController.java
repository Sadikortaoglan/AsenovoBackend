package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.StockGroupCreateRequest;
import com.saraasansor.api.dto.StockGroupPageResponse;
import com.saraasansor.api.dto.StockGroupResponse;
import com.saraasansor.api.dto.StockGroupUpdateRequest;
import com.saraasansor.api.service.StockGroupService;
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

@RestController
@RequestMapping("/stock-groups")
public class StockGroupController {

    private final StockGroupService stockGroupService;

    public StockGroupController(StockGroupService stockGroupService) {
        this.stockGroupService = stockGroupService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockGroupPageResponse>> getStockGroups(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                stockGroupService.getStockGroups(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockGroupResponse>> getStockGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stockGroupService.getStockGroupById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockGroupResponse>> createStockGroup(@Valid @RequestBody StockGroupCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock group created successfully", stockGroupService.createStockGroup(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockGroupResponse>> updateStockGroup(@PathVariable Long id,
                                                                             @Valid @RequestBody StockGroupUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock group updated successfully", stockGroupService.updateStockGroup(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteStockGroup(@PathVariable Long id) {
        stockGroupService.deleteStockGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Stock group deactivated successfully", null));
    }
}
