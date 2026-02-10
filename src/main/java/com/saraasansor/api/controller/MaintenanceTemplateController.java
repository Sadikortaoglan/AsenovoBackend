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
@RequestMapping("/maintenance/templates")
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
    public ResponseEntity<ApiResponse<MaintenanceSection>> createSection(
            @PathVariable Long templateId, @RequestBody MaintenanceSection section) {
        try {
            MaintenanceSection created = templateService.createSection(templateId, section);
            return ResponseEntity.ok(ApiResponse.success("Section created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<MaintenanceSection>> updateSection(
            @PathVariable Long id, @RequestBody MaintenanceSection section) {
        try {
            MaintenanceSection updated = templateService.updateSection(id, section);
            return ResponseEntity.ok(ApiResponse.success("Section updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/sections/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable Long id) {
        try {
            templateService.deleteSection(id);
            return ResponseEntity.ok(ApiResponse.success("Section deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Items
    @GetMapping("/sections/{sectionId}/items")
    public ResponseEntity<ApiResponse<List<MaintenanceItem>>> getItems(@PathVariable Long sectionId) {
        try {
            List<MaintenanceItem> items = templateService.getItemsBySectionId(sectionId);
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/sections/{sectionId}/items")
    public ResponseEntity<ApiResponse<MaintenanceItem>> createItem(
            @PathVariable Long sectionId, @RequestBody MaintenanceItem item) {
        try {
            MaintenanceItem created = templateService.createItem(sectionId, item);
            return ResponseEntity.ok(ApiResponse.success("Item created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MaintenanceItem>> updateItem(
            @PathVariable Long id, @RequestBody MaintenanceItem item) {
        try {
            MaintenanceItem updated = templateService.updateItem(id, item);
            return ResponseEntity.ok(ApiResponse.success("Item updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PatchMapping("/items/{id}/toggle-active")
    public ResponseEntity<ApiResponse<MaintenanceItem>> toggleItemActive(@PathVariable Long id) {
        try {
            MaintenanceItem updated = templateService.toggleItemActive(id);
            return ResponseEntity.ok(ApiResponse.success("Item status updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
