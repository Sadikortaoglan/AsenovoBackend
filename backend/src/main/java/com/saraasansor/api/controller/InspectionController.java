package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.InspectionDto;
import com.saraasansor.api.service.InspectionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inspections")
public class InspectionController {
    
    @Autowired
    private InspectionService inspectionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<InspectionDto>>> getAllInspections() {
        List<InspectionDto> inspections = inspectionService.getAllInspections();
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionDto>> getInspectionById(@PathVariable Long id) {
        try {
            InspectionDto inspection = inspectionService.getInspectionById(id);
            return ResponseEntity.ok(ApiResponse.success(inspection));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/elevator/{elevatorId}")
    public ResponseEntity<ApiResponse<List<InspectionDto>>> getInspectionsByElevatorId(
            @PathVariable Long elevatorId) {
        List<InspectionDto> inspections = inspectionService.getInspectionsByElevatorId(elevatorId);
        return ResponseEntity.ok(ApiResponse.success(inspections));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<InspectionDto>> createInspection(
            @Valid @RequestBody InspectionDto dto) {
        try {
            InspectionDto created = inspectionService.createInspection(dto);
            return ResponseEntity.ok(ApiResponse.success("Inspection record successfully added", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InspectionDto>> updateInspection(
            @PathVariable Long id, @Valid @RequestBody InspectionDto dto) {
        try {
            InspectionDto updated = inspectionService.updateInspection(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Inspection record successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInspection(@PathVariable Long id) {
        try {
            inspectionService.deleteInspection(id);
            return ResponseEntity.ok(ApiResponse.success("Inspection record successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

