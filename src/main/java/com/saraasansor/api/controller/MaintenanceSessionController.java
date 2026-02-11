package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.model.MaintenanceSession;
import com.saraasansor.api.model.MaintenanceStepResult;
import com.saraasansor.api.service.MaintenanceSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/maintenances")
public class MaintenanceSessionController {
    
    @Autowired
    private MaintenanceSessionService sessionService;
    
    @PostMapping("/sessions/start")
    public ResponseEntity<ApiResponse<MaintenanceSession>> startSession(@RequestBody StartSessionRequest request) {
        try {
            MaintenanceSession session = sessionService.startSession(
                    request.getElevatorId(),
                    request.getTemplateId(),
                    request.getQrToken(),
                    request.getPlanId());
            return ResponseEntity.ok(ApiResponse.success("Session started successfully", session));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<MaintenanceSessionService.SessionContext>> getSessionContext(@PathVariable Long id) {
        try {
            MaintenanceSessionService.SessionContext context = sessionService.getSessionContext(id);
            return ResponseEntity.ok(ApiResponse.success(context));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/sessions/{sessionId}/steps/{itemId}")
    public ResponseEntity<ApiResponse<MaintenanceStepResult>> updateStepResult(
            @PathVariable Long sessionId,
            @PathVariable Long itemId,
            @RequestBody StepResultRequest request) {
        try {
            MaintenanceStepResult result = sessionService.updateStepResult(
                    sessionId,
                    itemId,
                    MaintenanceStepResult.StepResult.valueOf(request.getResult()),
                    request.getNote());
            return ResponseEntity.ok(ApiResponse.success("Step result updated", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/sessions/{id}/finalize")
    public ResponseEntity<ApiResponse<MaintenanceSession>> finalizeSession(
            @PathVariable Long id,
            @RequestBody FinalizeSessionRequest request) {
        try {
            MaintenanceSession session = sessionService.finalizeSession(
                    id,
                    request.getOverallNote(),
                    request.getSignatureUrl());
            return ResponseEntity.ok(ApiResponse.success("Session finalized successfully", session));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/sessions/{id}/report")
    public ResponseEntity<ApiResponse<MaintenanceSessionService.SessionContext>> getSessionReport(@PathVariable Long id) {
        try {
            MaintenanceSessionService.SessionContext context = sessionService.getSessionContext(id);
            return ResponseEntity.ok(ApiResponse.success(context));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<Page<MaintenancePlan>>> getCompletedPlans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Default date range: last 30 days if not provided
            if (from == null) {
                from = LocalDate.now().minusDays(30);
            }
            if (to == null) {
                to = LocalDate.now();
            }
            
           
            // Convert to Page
            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), plans.size());
            List<MaintenancePlan> paginatedPlans = plans.subList(Math.min(start, plans.size()), end);
            Page<MaintenancePlan> pageResult = new PageImpl<>(
                    paginatedPlans, pageable, plans.size());
            
            return ResponseEntity.ok(ApiResponse.success(pageResult));
        } catch (Exception e) {
            System.err.println("getCompletedPlans error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<MaintenancePlan>>> getUpcomingPlans(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // Parse status if provided
            MaintenancePlan.PlanStatus planStatus = null;
            if (status != null && !status.isEmpty()) {
                try {
                    planStatus = MaintenancePlan.PlanStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid status: " + status + ". Valid values: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED"));
                }
            }
            
            
            List<MaintenancePlan> plans = sessionService.getUpcomingPlans(from, to, planStatus);
            
        
            // Simple pagination (can be improved)
            int start = page * size;
            int end = Math.min(start + size, plans.size());
            List<MaintenancePlan> paginatedPlans = plans.subList(Math.min(start, plans.size()), end);
            
            return ResponseEntity.ok(ApiResponse.success(paginatedPlans));
        } catch (Exception e) {
            System.err.println("getUpcomingPlans error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // Request DTOs
    public static class StartSessionRequest {
        private Long elevatorId;
        private Long templateId;
        private String qrToken;
        private Long planId;
        
        public Long getElevatorId() {
            return elevatorId;
        }
        
        public void setElevatorId(Long elevatorId) {
            this.elevatorId = elevatorId;
        }
        
        public Long getTemplateId() {
            return templateId;
        }
        
        public void setTemplateId(Long templateId) {
            this.templateId = templateId;
        }
        
        public String getQrToken() {
            return qrToken;
        }
        
        public void setQrToken(String qrToken) {
            this.qrToken = qrToken;
        }
        
        public Long getPlanId() {
            return planId;
        }
        
        public void setPlanId(Long planId) {
            this.planId = planId;
        }
    }
    
    public static class StepResultRequest {
        private String result; // COMPLETED, ISSUE_FOUND, NOT_APPLICABLE
        private String note;
        
        public String getResult() {
            return result;
        }
        
        public void setResult(String result) {
            this.result = result;
        }
        
        public String getNote() {
            return note;
        }
        
        public void setNote(String note) {
            this.note = note;
        }
    }
    
    public static class FinalizeSessionRequest {
        private String overallNote;
        private String signatureUrl;
        
        public String getOverallNote() {
            return overallNote;
        }
        
        public void setOverallNote(String overallNote) {
            this.overallNote = overallNote;
        }
        
        public String getSignatureUrl() {
            return signatureUrl;
        }
        
        public void setSignatureUrl(String signatureUrl) {
            this.signatureUrl = signatureUrl;
        }
    }
}
