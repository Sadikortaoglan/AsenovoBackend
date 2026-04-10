package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.dto.WarehouseCreateRequest;
import com.saraasansor.api.dto.WarehousePageResponse;
import com.saraasansor.api.dto.WarehouseResponse;
import com.saraasansor.api.dto.WarehouseUpdateRequest;
import com.saraasansor.api.service.WarehouseService;
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
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<WarehousePageResponse>> getWarehouses(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                warehouseService.getWarehouses(query, active, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouseById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getWarehouseById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> createWarehouse(@Valid @RequestBody WarehouseCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Warehouse created successfully", warehouseService.createWarehouse(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<WarehouseResponse>> updateWarehouse(@PathVariable Long id,
                                                                          @Valid @RequestBody WarehouseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Warehouse updated successfully", warehouseService.updateWarehouse(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable Long id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.ok(ApiResponse.success("Warehouse deactivated successfully", null));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','TENANT_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<List<LookupDto>>> getLookup(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getLookup(query)));
    }
}
