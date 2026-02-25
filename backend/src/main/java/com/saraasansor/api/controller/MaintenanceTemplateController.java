package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.model.MaintenanceItem;
import com.saraasansor.api.model.MaintenanceSection;
import com.saraasansor.api.model.MaintenanceTemplate;
import com.saraasansor.api.service.MaintenanceTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maintenance-templates")
public class MaintenanceTemplateController {
    
    @Autowired
    private MaintenanceTemplateService templateService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceTemplate>>> getAllTemplates() {
        List<MaintenanceTemplate> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<MaintenanceTemplate>>> getActiveTemplates() {
        List<MaintenanceTemplate> templates = templateService.getActiveTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> getTemplateById(@PathVariable Long id) {
        try {
            MaintenanceTemplate template = templateService.getTemplateById(id);
            return ResponseEntity.ok(ApiResponse.success(template));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> createTemplate(@RequestBody MaintenanceTemplate template) {
        try {
            MaintenanceTemplate created = templateService.createTemplate(template);
            return ResponseEntity.ok(ApiResponse.success("Template created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> updateTemplate(
            @PathVariable Long id, @RequestBody MaintenanceTemplate template) {
        try {
            MaintenanceTemplate updated = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(ApiResponse.success("Template updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> duplicateTemplate(@PathVariable Long id) {
        try {
            MaintenanceTemplate duplicated = templateService.duplicateTemplate(id);
            return ResponseEntity.ok(ApiResponse.success("Template duplicated successfully", duplicated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.ok(ApiResponse.success("Template deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Sections
    @GetMapping("/{templateId}/sections")
    public ResponseEntity<ApiResponse<List<MaintenanceSection>>> getSections(@PathVariable Long templateId) {
        try {
            List<MaintenanceSection> sections = templateService.getSectionsByTemplateId(templateId);
            return ResponseEntity.ok(ApiResponse.success(sections));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{templateId}/sections")
    public ResponseEntity<ApiResponse<MaintenanceTemplate>> createSection(
            @PathVariable Long templateId, @RequestBody MaintenanceSection section) {
        try {
            MaintenanceTemplate updatedTemplate = templateService.createSection(templateId, section);
            return ResponseEntity.status(201).body(ApiResponse.success("Section created successfully", updatedTemplate));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
}
