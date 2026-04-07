package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.StockUnitCreateRequest;
import com.saraasansor.api.dto.StockUnitLookupDto;
import com.saraasansor.api.dto.StockUnitPageResponse;
import com.saraasansor.api.dto.StockUnitResponse;
import com.saraasansor.api.dto.StockUnitUpdateRequest;
import com.saraasansor.api.service.StockUnitService;
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
@RequestMapping("/stock-units")
public class StockUnitController {

    private final StockUnitService stockUnitService;

    public StockUnitController(StockUnitService stockUnitService) {
        this.stockUnitService = stockUnitService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockUnitPageResponse>> getStockUnits(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                stockUnitService.getStockUnits(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockUnitResponse>> getStockUnitById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(stockUnitService.getStockUnitById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockUnitResponse>> createStockUnit(@Valid @RequestBody StockUnitCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock unit created successfully", stockUnitService.createStockUnit(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<StockUnitResponse>> updateStockUnit(@PathVariable Long id,
                                                                           @Valid @RequestBody StockUnitUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stock unit updated successfully", stockUnitService.updateStockUnit(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteStockUnit(@PathVariable Long id) {
        stockUnitService.deleteStockUnit(id);
        return ResponseEntity.ok(ApiResponse.success("Stock unit deactivated successfully", null));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<List<StockUnitLookupDto>>> getLookup(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(stockUnitService.getLookup(query)));
    }
}
