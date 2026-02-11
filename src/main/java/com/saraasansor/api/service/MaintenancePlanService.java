package com.saraasansor.api.service;

import com.saraasansor.api.dto.CreateMaintenancePlanRequest;
import com.saraasansor.api.dto.MaintenancePlanResponseDto;
import com.saraasansor.api.dto.RescheduleMaintenancePlanRequest;
import com.saraasansor.api.dto.UpdateMaintenancePlanRequest;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.model.MaintenanceTemplate;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.MaintenancePlanRepository;
import com.saraasansor.api.repository.MaintenanceTemplateRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenancePlanService {
    
    @Autowired
    private MaintenancePlanRepository planRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private MaintenanceTemplateRepository templateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.saraasansor.api.service.QrProofService qrProofService;
    
    @Autowired
    private com.saraasansor.api.repository.QrProofRepository qrProofRepository;
    
    public List<MaintenancePlan> getPlansByDateRange(LocalDate from, LocalDate to) {
        return planRepository.findByPlannedDateBetweenOrderByPlannedDateAsc(
            from, to, 
            java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
    }
    
    public List<MaintenancePlan> getPlansByElevatorAndDateRange(Long elevatorId, LocalDate from, LocalDate to) {
        return planRepository.findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
            elevatorId, from, to,
            java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
    }
    
    public List<MaintenancePlan> getPlansByTechnicianAndDateRange(Long technicianId, LocalDate from, LocalDate to) {
        return planRepository.findByAssignedTechnicianIdAndPlannedDateBetweenOrderByPlannedDateAsc(technicianId, from, to);
    }
    
    public List<MaintenancePlan> getPlansByStatusAndDateRange(MaintenancePlan.PlanStatus status, LocalDate from, LocalDate to) {
        return planRepository.findByPlannedDateBetweenAndStatusOrderByPlannedDateAsc(from, to, status);
    }
    
    public MaintenancePlan getPlanById(Long id) {
        return planRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Maintenance plan not found"));
    }
    
    public MaintenancePlanResponseDto createPlan(CreateMaintenancePlanRequest request) {
        // Validate elevator exists
        Elevator elevator = elevatorRepository.findById(request.getElevatorId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        // Validate template exists
        MaintenanceTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Maintenance template not found"));
        
        // Validate monthly uniqueness
        validateMonthlyUniqueness(request.getElevatorId(), request.getPlannedDate());
        
        // Validate planned date is in future
        if (request.getPlannedDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Planned date must be in the future");
        }
        
        MaintenancePlan plan = new MaintenancePlan();
        plan.setElevator(elevator);
        plan.setTemplate(template);
        plan.setPlannedDate(request.getPlannedDate());
        plan.setStatus(MaintenancePlan.PlanStatus.PLANNED);
        
        // Validate technician if provided
        if (request.getAssignedTechnicianId() != null) {
            User technician = userRepository.findById(request.getAssignedTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            plan.setAssignedTechnician(technician);
        }
        
        MaintenancePlan saved = planRepository.save(plan);
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    /**
     * Validate monthly uniqueness for maintenance plans
     * Rules:
     * - Only PLANNED and IN_PROGRESS plans count as "active"
     * - COMPLETED and NOT_PLANNED plans don't count (can have multiple)
     * - Same elevator can have multiple plans in same month if others are COMPLETED/NOT_PLANNED
     */
    private void validateMonthlyUniqueness(Long elevatorId, LocalDate plannedDate) {
        YearMonth yearMonth = YearMonth.from(plannedDate);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        
        // Query already filters by PLANNED and IN_PROGRESS at repository level
        List<MaintenancePlan> existingPlans = planRepository.findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
            elevatorId, start, end,
            java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
        
        if (!existingPlans.isEmpty()) {
            MaintenancePlan conflictingPlan = existingPlans.get(0);
            String errorMsg = "Each elevator can have only one active (PLANNED or IN_PROGRESS) maintenance per calendar month. " +
                    "Existing plan ID: " + conflictingPlan.getId() + " with status: " + conflictingPlan.getStatus();
            throw new RuntimeException(errorMsg);
        }
    }
    
    /**
     * Validate monthly uniqueness for update/reschedule operations
     * Excludes the current plan from validation
     */
    private void validateMonthlyUniquenessExcluding(Long elevatorId, LocalDate plannedDate, Long excludePlanId) {
        YearMonth yearMonth = YearMonth.from(plannedDate);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        
        // Query already filters by PLANNED and IN_PROGRESS at repository level
        List<MaintenancePlan> existingPlans = planRepository.findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
            elevatorId, start, end, java.util.Arrays.asList(
                MaintenancePlan.PlanStatus.PLANNED, 
                MaintenancePlan.PlanStatus.IN_PROGRESS));
        
        // Exclude current plan
        List<MaintenancePlan> conflictingPlans = existingPlans.stream()
            .filter(p -> !p.getId().equals(excludePlanId))
            .collect(Collectors.toList());
        
        if (!conflictingPlans.isEmpty()) {
            MaintenancePlan conflictingPlan = conflictingPlans.get(0);
            String errorMsg = "Each elevator can have only one active (PLANNED or IN_PROGRESS) maintenance per calendar month. " +
                    "Existing plan ID: " + conflictingPlan.getId() + " with status: " + conflictingPlan.getStatus();
            throw new RuntimeException(errorMsg);
        }
    }
    
    /**
     * Get all maintenance plans (PLANNED + IN_PROGRESS only)
     * SQL-level filtering - NOT_PLANNED and COMPLETED never returned
     * Used for calendar view
     */
    public List<MaintenancePlanResponseDto> getAllPlans(String month, Integer year, Long elevatorId, String status) {
        LocalDate from;
        LocalDate to;
        
        if (month != null && !month.isEmpty()) {
            try {
                YearMonth yearMonth = YearMonth.parse(month);
                from = yearMonth.atDay(1);
                to = yearMonth.atEndOfMonth();
            } catch (Exception e) {
                throw new RuntimeException("Invalid month format. Expected YYYY-MM (e.g., 2026-02)");
            }
        } else if (year != null) {
            from = LocalDate.of(year, 1, 1);
            to = LocalDate.of(year, 12, 31);
        } else {
            YearMonth currentMonth = YearMonth.now();
            from = currentMonth.atDay(1);
            to = currentMonth.atEndOfMonth();
        }
        
        List<MaintenancePlan> plans;
        
        // SQL-level filtering: Only PLANNED and IN_PROGRESS
        // NOT_PLANNED and COMPLETED excluded at SQL level (NOT_PLANNED has plannedDate = null, so won't match date range)
        if (elevatorId != null) {
            plans = planRepository.findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
                elevatorId, from, to, 
                java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
        } else {
            plans = planRepository.findByPlannedDateBetweenOrderByPlannedDateAsc(
                from, to, 
                java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
        }
        
        return plans.stream()
            .map(MaintenancePlanResponseDto::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Get completed maintenance plans
     * SQL-level filtering: Only COMPLETED status
     * Ordered by completedAt DESC
     */
    public List<MaintenancePlanResponseDto> getCompletedPlans(LocalDateTime from, LocalDateTime to) {
        List<MaintenancePlan> plans;
        
        if (from != null && to != null) {
            plans = planRepository.findByStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
                MaintenancePlan.PlanStatus.COMPLETED, from, to);
        } else {
            plans = planRepository.findByStatusOrderByCompletedAtDesc(
                MaintenancePlan.PlanStatus.COMPLETED);
        }
        
        return plans.stream()
            .map(MaintenancePlanResponseDto::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Update maintenance plan
     * Rules:
     * - Only PLANNED status can be updated
     * - COMPLETED status cannot have date changed
     */
    public MaintenancePlanResponseDto updatePlan(Long id, UpdateMaintenancePlanRequest request) {
        MaintenancePlan plan = getPlanById(id);
        
        // Validation: Only PLANNED status can be updated
        if (plan.getStatus() != MaintenancePlan.PlanStatus.PLANNED) {
            throw new RuntimeException("Only PLANNED maintenance plans can be updated. Current status: " + plan.getStatus());
        }
        
        // Get current user for audit
        User currentUser = getCurrentUser();
        
        // Update planned date if provided
        if (request.getPlannedDate() != null) {
            // Validation: Cannot schedule in the past
            if (request.getPlannedDate().isBefore(LocalDate.now())) {
                throw new RuntimeException("Planned date cannot be in the past");
            }
            
            // Validation: Monthly uniqueness (exclude current plan)
            validateMonthlyUniquenessExcluding(plan.getElevator().getId(), request.getPlannedDate(), id);
            
            plan.setPlannedDate(request.getPlannedDate());
        }
        
        // Update template if provided
        if (request.getTemplateId() != null) {
            MaintenanceTemplate template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Maintenance template not found"));
            plan.setTemplate(template);
        }
        
        // Update technician if provided
        if (request.getTechnicianId() != null) {
            User technician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            plan.setAssignedTechnician(technician);
        }
        
        // Audit log
        plan.setUpdatedBy(currentUser);
        plan.setUpdatedAt(LocalDateTime.now());
        
        MaintenancePlan saved = planRepository.save(plan);
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    /**
     * Reschedule maintenance plan (only date change)
     * Rules:
     * - Only PLANNED status can be rescheduled
     * - COMPLETED status cannot be rescheduled
     */
    public MaintenancePlanResponseDto reschedulePlan(Long id, RescheduleMaintenancePlanRequest request) {
        MaintenancePlan plan = getPlanById(id);
        
        // Validation: Only PLANNED status can be rescheduled
        if (plan.getStatus() != MaintenancePlan.PlanStatus.PLANNED) {
            throw new RuntimeException("Only PLANNED maintenance plans can be rescheduled. Current status: " + plan.getStatus());
        }
        
        // Validation: Cannot schedule in the past
        if (request.getPlannedDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Planned date cannot be in the past");
        }
        
        // Validation: Monthly uniqueness (exclude current plan)
        validateMonthlyUniquenessExcluding(plan.getElevator().getId(), request.getPlannedDate(), id);
        
        // Update date
        plan.setPlannedDate(request.getPlannedDate());
        
        // Audit log
        User currentUser = getCurrentUser();
        plan.setUpdatedBy(currentUser);
        plan.setUpdatedAt(LocalDateTime.now());
        
        MaintenancePlan saved = planRepository.save(plan);
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    /**
     * Start maintenance plan (PLANNED → IN_PROGRESS)
     * RULE 1: Requires QR proof
     */
    public MaintenancePlanResponseDto startPlan(Long id, String qrToken) {
        MaintenancePlan plan = getPlanById(id);
        
        // Validation: Only PLANNED can be started
        if (plan.getStatus() != MaintenancePlan.PlanStatus.PLANNED) {
            throw new RuntimeException("Only PLANNED maintenance plans can be started. Current status: " + plan.getStatus());
        }
        
        // Validation: QR proof required
        if (qrToken == null || qrToken.isEmpty()) {
            throw new RuntimeException("QR token is required to start maintenance");
        }
        
        // Validate and use QR token
        User currentUser = getCurrentUser();
        com.saraasansor.api.model.QrProof qrProof = qrProofService.validateAndUseToken(qrToken, currentUser.getId());
        
        // Verify QR proof is for the same elevator
        if (!qrProof.getElevator().getId().equals(plan.getElevator().getId())) {
            throw new RuntimeException("QR token is for a different elevator");
        }
        
        // Update plan
        plan.setStatus(MaintenancePlan.PlanStatus.IN_PROGRESS);
        plan.setQrProof(qrProof);
        plan.setUpdatedBy(currentUser);
        plan.setUpdatedAt(LocalDateTime.now());
        
        MaintenancePlan saved = planRepository.save(plan);
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    /**
     * Complete maintenance plan (IN_PROGRESS → COMPLETED)
     * RULE 2: Only IN_PROGRESS can be completed
     * RULE 3: Requires QR proof ID and at least 4 photos
     */
    public MaintenancePlanResponseDto completePlan(Long id, Long qrProofId, java.util.List<String> photoUrls, String note, java.math.BigDecimal price) {
        MaintenancePlan plan = getPlanById(id);
        
        // Validation: Only IN_PROGRESS can be completed
        if (plan.getStatus() != MaintenancePlan.PlanStatus.IN_PROGRESS) {
            throw new RuntimeException("Only IN_PROGRESS maintenance plans can be completed. Current status: " + plan.getStatus());
        }
        
        // Validation: QR proof required
        if (qrProofId == null) {
            throw new RuntimeException("QR proof ID is required to complete maintenance");
        }
        
        // Validation: At least 4 photos required
        if (photoUrls == null || photoUrls.size() < 4) {
            throw new RuntimeException("At least 4 photos are required to complete maintenance");
        }
        
        // Verify QR proof exists and matches plan
        com.saraasansor.api.model.QrProof qrProof = qrProofRepository.findById(qrProofId)
            .orElseThrow(() -> new RuntimeException("QR proof not found"));
        
        if (!qrProof.getElevator().getId().equals(plan.getElevator().getId())) {
            throw new RuntimeException("QR proof is for a different elevator");
        }
        
        // Update plan
        plan.setStatus(MaintenancePlan.PlanStatus.COMPLETED);
        plan.setQrProof(qrProof);
        plan.setCompletedAt(LocalDateTime.now());
        plan.setNote(note);
        plan.setPrice(price);
        plan.setUpdatedBy(getCurrentUser());
        plan.setUpdatedAt(LocalDateTime.now());
        
        MaintenancePlan saved = planRepository.save(plan);
        
        // TODO: Save photos to maintenance_plan_photos table
        
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    public MaintenancePlanResponseDto getPlanByIdDto(Long id) {
        MaintenancePlan plan = getPlanById(id);
        return MaintenancePlanResponseDto.fromEntity(plan);
    }
    
    /**
     * Cancel maintenance plan (reset to NOT_PLANNED)
     * Business rule: Cancelled plan behaves as if it was never planned
     * Rules:
     * - Only PLANNED status can be cancelled
     * - IN_PROGRESS and COMPLETED cannot be cancelled
     * - Sets: plannedDate = null, assignedTechnician = null, status = NOT_PLANNED
     * Returns updated plan for frontend refresh
     */
    @Transactional
    public MaintenancePlanResponseDto deletePlan(Long id) {
        MaintenancePlan plan = getPlanById(id);
        
        // Validation: Only PLANNED status can be cancelled
        if (plan.getStatus() == MaintenancePlan.PlanStatus.IN_PROGRESS || 
            plan.getStatus() == MaintenancePlan.PlanStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel maintenance plan with status: " + plan.getStatus() + 
                    ". Only PLANNED plans can be cancelled.");
        }
        
        // Cancel: Reset to NOT_PLANNED state
        // This makes the plan behave as if it was never planned
        plan.setStatus(MaintenancePlan.PlanStatus.NOT_PLANNED);
        plan.setPlannedDate(null);
        plan.setAssignedTechnician(null);
        
        // Audit log
        User currentUser = getCurrentUser();
        plan.setCancelledBy(currentUser);
        plan.setCancelledAt(LocalDateTime.now());
        plan.setUpdatedBy(currentUser);
        plan.setUpdatedAt(LocalDateTime.now());
        
        System.out.println("========================================");
        System.out.println("CANCELLING PLAN ID: " + id);
        System.out.println("Setting: status = NOT_PLANNED, plannedDate = null, assignedTechnician = null");
        System.out.println("========================================");
        
        // Save and flush to ensure immediate database write
        MaintenancePlan saved = planRepository.save(plan);
        planRepository.flush(); // Force immediate database write - CRITICAL for persistence
        
        System.out.println("========================================");
        System.out.println("SAVED AND FLUSHED. Verifying persistence...");
        System.out.println("========================================");
        
        // Verify all fields were persisted correctly
        MaintenancePlan verify = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found after cancellation"));
        
        System.out.println("========================================");
        System.out.println("VERIFICATION QUERY RESULT:");
        System.out.println("Plan ID: " + verify.getId());
        System.out.println("Plan Status: " + verify.getStatus());
        System.out.println("Plan PlannedDate: " + verify.getPlannedDate());
        System.out.println("Plan AssignedTechnician: " + (verify.getAssignedTechnician() == null ? "null" : verify.getAssignedTechnician().getId()));
        System.out.println("Expected: status = NOT_PLANNED, plannedDate = null, assignedTechnician = null");
        System.out.println("========================================");
        
        // Verify status
        if (verify.getStatus() != MaintenancePlan.PlanStatus.NOT_PLANNED) {
            throw new RuntimeException("CRITICAL: Status was not persisted. Expected NOT_PLANNED, got: " + verify.getStatus());
        }
        
        // Verify plannedDate is null
        if (verify.getPlannedDate() != null) {
            throw new RuntimeException("CRITICAL: PlannedDate was not cleared. Expected null, got: " + verify.getPlannedDate());
        }
        
        // Verify assignedTechnician is null
        if (verify.getAssignedTechnician() != null) {
            throw new RuntimeException("CRITICAL: AssignedTechnician was not cleared. Expected null, got: " + verify.getAssignedTechnician().getId());
        }
        
        System.out.println("========================================");
        System.out.println("PERSISTENCE VERIFIED: NOT_PLANNED, plannedDate = null, assignedTechnician = null");
        System.out.println("========================================");
        
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
