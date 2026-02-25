package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.FaultDto;
import com.saraasansor.api.service.FaultService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faults")
public class FaultController {
    
    @Autowired
    private FaultService faultService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<FaultDto>>> getFaults(
            @RequestParam(required = false) String status) {
        try {
            List<FaultDto> faults = faultService.getFaultsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(faults));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FaultDto>> getFaultById(@PathVariable Long id) {
        try {
            FaultDto fault = faultService.getFaultById(id);
            return ResponseEntity.ok(ApiResponse.success(fault));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<FaultDto>> createFault(@Valid @RequestBody FaultDto dto) {
        try {
            FaultDto created = faultService.createFault(dto);
            return ResponseEntity.ok(ApiResponse.success("Fault record successfully added", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FaultDto>> updateFaultStatus(
            @PathVariable Long id, @RequestParam String status) {
        try {
            FaultDto updated = faultService.updateFaultStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Fault status updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FaultDto>> updateFault(
            @PathVariable Long id, @Valid @RequestBody FaultDto dto) {
        try {
            FaultDto updated = faultService.updateFault(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Fault record successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFault(@PathVariable Long id) {
        try {
            faultService.deleteFault(id);
            return ResponseEntity.ok(ApiResponse.success("Fault record successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

