package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CompleteMaintenancePlanRequest;
import com.saraasansor.api.dto.CreateMaintenancePlanRequest;
import com.saraasansor.api.dto.MaintenancePlanResponseDto;
import com.saraasansor.api.dto.RescheduleMaintenancePlanRequest;
import com.saraasansor.api.dto.StartMaintenancePlanRequest;
import com.saraasansor.api.dto.UpdateMaintenancePlanRequest;
import com.saraasansor.api.service.MaintenancePlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/maintenance-plans")
public class MaintenancePlanController {
    
    @Autowired
    private MaintenancePlanService planService;
    
    /**
     * CREATE - Plan Oluşturma
     * POST /api/maintenance-plans
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> createPlan(
            @Valid @RequestBody CreateMaintenancePlanRequest request) {
        try {
            MaintenancePlanResponseDto created = planService.createPlan(request);
            return ResponseEntity.ok(ApiResponse.success("Plan created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * GET ALL - Planları Listeleme
     * GET /api/maintenance-plans?month=2026-02&elevatorId=15&status=PLANNED
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenancePlanResponseDto>>> getAllPlans(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long elevatorId,
            @RequestParam(required = false) String status) {
        try {
        
            List<MaintenancePlanResponseDto> plans = planService.getAllPlans(month, year, elevatorId, status);
            
        
            return ResponseEntity.ok(ApiResponse.success("Plans retrieved successfully", plans));
        } catch (Exception e) {
            System.err.println("ERROR in getAllPlans: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * GET BY ID - Tek Plan Detayı
     * GET /api/maintenance-plans/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> getPlanById(@PathVariable Long id) {
        try {
            MaintenancePlanResponseDto plan = planService.getPlanByIdDto(id);
            return ResponseEntity.ok(ApiResponse.success("Plan retrieved successfully", plan));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * UPDATE - Plan Güncelleme
     * PUT /api/maintenance-plans/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaintenancePlanRequest request) {
        try {
            MaintenancePlanResponseDto updated = planService.updatePlan(id, request);
            return ResponseEntity.ok(ApiResponse.success("Plan updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * RESCHEDULE - Plan Tarihini Değiştirme
     * PATCH /api/maintenance-plans/{id}/reschedule
     */
    @PatchMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> reschedulePlan(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleMaintenancePlanRequest request) {
        try {
            MaintenancePlanResponseDto rescheduled = planService.reschedulePlan(id, request);
            return ResponseEntity.ok(ApiResponse.success("Plan rescheduled successfully", rescheduled));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * DELETE - Cancel Plan (Reset to NOT_PLANNED)
     * DELETE /api/maintenance-plans/{id}
     * Sets: status = NOT_PLANNED, plannedDate = null, assignedTechnician = null
     * Returns updated plan for frontend refresh
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> deletePlan(@PathVariable Long id) {
        try {
            MaintenancePlanResponseDto cancelled = planService.deletePlan(id);
            return ResponseEntity.ok(ApiResponse.success("Plan cancelled successfully", cancelled));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * START - Start maintenance (PLANNED → IN_PROGRESS)
     * POST /api/maintenance-plans/{id}/start
     * Requires QR proof
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> startPlan(
            @PathVariable Long id,
            @Valid @RequestBody StartMaintenancePlanRequest request) {
        try {
            MaintenancePlanResponseDto started = planService.startPlan(id, request.getQrToken());
            return ResponseEntity.ok(ApiResponse.success("Maintenance started successfully", started));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * COMPLETE - Complete maintenance (IN_PROGRESS → COMPLETED)
     * POST /api/maintenance-plans/{id}/complete
     * Requires QR proof ID and at least 4 photos
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<MaintenancePlanResponseDto>> completePlan(
            @PathVariable Long id,
            @Valid @RequestBody CompleteMaintenancePlanRequest request) {
        try {
            MaintenancePlanResponseDto completed = planService.completePlan(
                id, request.getQrProofId(), request.getPhotoUrls(), request.getNote(), request.getPrice());
            return ResponseEntity.ok(ApiResponse.success("Maintenance completed successfully", completed));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * GET COMPLETED - Get completed maintenance plans
     * GET /api/maintenance-plans/completed
     */
    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<List<MaintenancePlanResponseDto>>> getCompletedPlans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            List<MaintenancePlanResponseDto> completed = planService.getCompletedPlans(from, to);
            return ResponseEntity.ok(ApiResponse.success("Completed plans retrieved successfully", completed));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
