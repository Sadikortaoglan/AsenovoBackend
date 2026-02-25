package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.model.Part;
import com.saraasansor.api.service.PartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parts")
public class PartController {
    
    @Autowired
    private PartService partService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Part>>> getAllParts() {
        List<Part> parts = partService.getAllParts();
        return ResponseEntity.ok(ApiResponse.success(parts));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Part>> getPartById(@PathVariable Long id) {
        try {
            Part part = partService.getPartById(id);
            return ResponseEntity.ok(ApiResponse.success(part));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Part>> createPart(@Valid @RequestBody Part part) {
        try {
            Part created = partService.createPart(part);
            return ResponseEntity.ok(ApiResponse.success("Part successfully added", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Part>> updatePart(
            @PathVariable Long id, @Valid @RequestBody Part part) {
        try {
            Part updated = partService.updatePart(id, part);
            return ResponseEntity.ok(ApiResponse.success("Part successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePart(@PathVariable Long id) {
        try {
            partService.deletePart(id);
            return ResponseEntity.ok(ApiResponse.success("Part successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

