package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.model.MaintenanceItem;
import com.saraasansor.api.model.MaintenanceTemplate;
import com.saraasansor.api.service.MaintenanceTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/maintenance-items")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TECHNICIAN')")
public class MaintenanceItemController {
    
    @Autowired
    private MaintenanceTemplateService templateService;
    
    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> updateItem(
            @PathVariable Long itemId, @RequestBody MaintenanceItem item) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.updateItem(itemId, item);
            return ResponseEntity.ok(ApiResponse.success("Item updated successfully", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> deleteItem(@PathVariable Long itemId) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.deleteItem(itemId);
            return ResponseEntity.ok(ApiResponse.success("Item deleted successfully", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PatchMapping("/{itemId}/toggle-active")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> toggleItemActive(@PathVariable Long itemId) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.toggleItemActive(itemId);
            return ResponseEntity.ok(ApiResponse.success("Item status updated", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
