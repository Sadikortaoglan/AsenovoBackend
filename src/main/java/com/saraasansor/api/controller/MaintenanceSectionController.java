package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.model.MaintenanceItem;
import com.saraasansor.api.model.MaintenanceSection;
import com.saraasansor.api.model.MaintenanceTemplate;
import com.saraasansor.api.service.MaintenanceTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/maintenance-sections")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'TECHNICIAN')")
public class MaintenanceSectionController {
    
    @Autowired
    private MaintenanceTemplateService templateService;
    
    @PutMapping("/{sectionId}")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> updateSection(
            @PathVariable Long sectionId, @RequestBody MaintenanceSection section) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.updateSection(sectionId, section);
            return ResponseEntity.ok(ApiResponse.success("Section updated successfully", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{sectionId}")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> deleteSection(@PathVariable Long sectionId) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.deleteSection(sectionId);
            return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{sectionId}/items")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> createItem(
            @PathVariable Long sectionId, @RequestBody MaintenanceItem item) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.createItem(sectionId, item);
            return ResponseEntity.status(201).body(ApiResponse.success("Item created successfully", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
