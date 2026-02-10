package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.service.MaintenancePlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/maintenances")
public class MaintenancePlanController {
    
    @Autowired
    private MaintenancePlanService planService;
    
    @GetMapping("/planning")
    public ResponseEntity<ApiResponse<List<MaintenancePlan>>> getPlanning(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long regionId) {
        try {
            List<MaintenancePlan> plans = planService.getPlansByDateRange(from, to);
            return ResponseEntity.ok(ApiResponse.success(plans));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<MaintenancePlan>> getPlanById(@PathVariable Long id) {
        try {
            MaintenancePlan plan = planService.getPlanById(id);
            return ResponseEntity.ok(ApiResponse.success(plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<MaintenancePlan>> createPlan(@RequestBody MaintenancePlan plan) {
        try {
            MaintenancePlan created = planService.createPlan(plan);
            return ResponseEntity.ok(ApiResponse.success("Plan created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/plans/bulk")
    public ResponseEntity<ApiResponse<List<MaintenancePlan>>> createBulkPlans(@RequestBody List<MaintenancePlan> plans) {
        try {
            List<MaintenancePlan> created = planService.createBulkPlans(plans);
            return ResponseEntity.ok(ApiResponse.success("Plans created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PatchMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<MaintenancePlan>> updatePlan(
            @PathVariable Long id, @RequestBody MaintenancePlan plan) {
        try {
            MaintenancePlan updated = planService.updatePlan(id, plan);
            return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/plans/{id}/cancel")
    public ResponseEntity<ApiResponse<MaintenancePlan>> cancelPlan(@PathVariable Long id) {
        try {
            MaintenancePlan cancelled = planService.cancelPlan(id);
            return ResponseEntity.ok(ApiResponse.success("Plan cancelled successfully", cancelled));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(@PathVariable Long id) {
        try {
            planService.deletePlan(id);
            return ResponseEntity.ok(ApiResponse.success("Plan deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
