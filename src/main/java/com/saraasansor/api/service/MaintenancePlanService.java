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
     * - COMPLETED and CANCELLED plans don't count (can have multiple)
     * - Same elevator can have multiple plans in same month if others are COMPLETED/CANCELLED
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
     * Get all maintenance plans with filtering
     * Default behavior: Only return PLANNED and IN_PROGRESS (active plans) for calendar view
     * If status parameter is provided, return plans with that specific status
     */
    public List<MaintenancePlanResponseDto> getAllPlans(String month, Integer year, Long elevatorId, String status) {
        try {
            LocalDate from;
            LocalDate to;
            
            if (month != null && !month.isEmpty()) {
                // Parse YYYY-MM format
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
                // Default: current month
                YearMonth currentMonth = YearMonth.now();
                from = currentMonth.atDay(1);
                to = currentMonth.atEndOfMonth();
            }
            
            List<MaintenancePlan> plans;
            
            if (elevatorId != null && status != null) {
                // Specific status requested - use all statuses query
                try {
                    MaintenancePlan.PlanStatus planStatus = MaintenancePlan.PlanStatus.valueOf(status.toUpperCase());
                    plans = planRepository.findByElevatorIdAndPlannedDateBetweenAllStatuses(elevatorId, from, to)
                        .stream()
                        .filter(p -> p.getStatus() == planStatus)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid status: " + status + ". Valid values: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED");
                }
            } else if (elevatorId != null) {
                // Elevator filter without status - default to active plans only
                // Repository query already filters by status IN ('PLANNED', 'IN_PROGRESS')
                plans = planRepository.findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(
                    elevatorId, from, to, 
                    java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
            } else if (status != null) {
                // Specific status requested
                try {
                    MaintenancePlan.PlanStatus planStatus = MaintenancePlan.PlanStatus.valueOf(status.toUpperCase());
                    plans = planRepository.findByPlannedDateBetweenAndStatusOrderByPlannedDateAsc(from, to, planStatus);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid status: " + status + ". Valid values: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED");
                }
            } else {
                // No filters - default to active plans only (PLANNED and IN_PROGRESS)
                // Repository query already filters by status IN ('PLANNED', 'IN_PROGRESS')
                // CANCELLED and COMPLETED are excluded at SQL level
                System.out.println("========================================");
                System.out.println("CALLING: planRepository.findByPlannedDateBetweenOrderByPlannedDateAsc");
                System.out.println("Date range: from=" + from + ", to=" + to);
                System.out.println("This method filters: status IN (PLANNED, IN_PROGRESS)");
                System.out.println("========================================");
                plans = planRepository.findByPlannedDateBetweenOrderByPlannedDateAsc(
                    from, to, 
                    java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
                System.out.println("========================================");
                System.out.println("QUERY RESULT: " + plans.size() + " plans returned from database");
                System.out.println("Plan IDs: " + plans.stream().map(p -> p.getId()).collect(Collectors.toList()));
                System.out.println("Plan statuses: " + plans.stream().map(p -> p.getStatus().name()).collect(Collectors.toList()));
                System.out.println("========================================");
            }
            
            List<MaintenancePlanResponseDto> result = plans.stream()
                .map(MaintenancePlanResponseDto::fromEntity)
                .collect(Collectors.toList());
            
            System.out.println("========================================");
            System.out.println("FINAL RESULT: " + result.size() + " DTOs");
            System.out.println("DTO IDs: " + result.stream().map(d -> d.getId()).collect(Collectors.toList()));
            System.out.println("DTO statuses: " + result.stream().map(d -> d.getStatus()).collect(Collectors.toList()));
            System.out.println("========================================");
            
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving maintenance plans: " + e.getMessage(), e);
        }
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
    
    public MaintenancePlanResponseDto completePlanWithQr(Long id, String qrCode) {
        MaintenancePlan plan = getPlanById(id);
        
        // TODO: Validate QR code
        // For now, just mark as completed
        plan.setStatus(MaintenancePlan.PlanStatus.COMPLETED);
        
        MaintenancePlan saved = planRepository.save(plan);
        MaintenancePlanResponseDto dto = MaintenancePlanResponseDto.fromEntity(saved);
        dto.setQrCode(qrCode);
        dto.setCompletedDate(java.time.LocalDateTime.now());
        return dto;
    }
    
    public MaintenancePlanResponseDto cancelPlan(Long id) {
        MaintenancePlan plan = getPlanById(id);
        plan.setStatus(MaintenancePlan.PlanStatus.CANCELLED);
        MaintenancePlan saved = planRepository.save(plan);
        return MaintenancePlanResponseDto.fromEntity(saved);
    }
    
    public MaintenancePlanResponseDto getPlanByIdDto(Long id) {
        MaintenancePlan plan = getPlanById(id);
        return MaintenancePlanResponseDto.fromEntity(plan);
    }
    
    /**
     * Soft delete maintenance plan (set status to CANCELLED)
     * Rules:
     * - Only PLANNED status can be deleted
     * - IN_PROGRESS and COMPLETED cannot be deleted
     * Returns updated plan for frontend refresh
     */
    public MaintenancePlanResponseDto deletePlan(Long id) {
        MaintenancePlan plan = getPlanById(id);
        
        // Validation: Only PLANNED status can be deleted
        if (plan.getStatus() == MaintenancePlan.PlanStatus.IN_PROGRESS || 
            plan.getStatus() == MaintenancePlan.PlanStatus.COMPLETED) {
            throw new RuntimeException("Cannot delete maintenance plan with status: " + plan.getStatus() + 
                    ". Only PLANNED plans can be deleted.");
        }
        
        // Soft delete: Set status to CANCELLED
        plan.setStatus(MaintenancePlan.PlanStatus.CANCELLED);
        
        // Audit log
        User currentUser = getCurrentUser();
        plan.setCancelledBy(currentUser);
        plan.setCancelledAt(LocalDateTime.now());
        plan.setUpdatedBy(currentUser);
        plan.setUpdatedAt(LocalDateTime.now());
        
        // Save and flush to ensure immediate database write
        System.out.println("========================================");
        System.out.println("CANCELLING PLAN ID: " + id);
        System.out.println("Current status: " + plan.getStatus());
        System.out.println("Setting status to: CANCELLED");
        System.out.println("========================================");
        
        MaintenancePlan saved = planRepository.save(plan);
        planRepository.flush(); // Force immediate database write - CRITICAL for status persistence
        
        System.out.println("========================================");
        System.out.println("SAVED AND FLUSHED. Verifying persistence...");
        System.out.println("========================================");
        
        // Verify status was persisted
        MaintenancePlan verify = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found after cancellation"));
        
        System.out.println("========================================");
        System.out.println("VERIFICATION QUERY RESULT:");
        System.out.println("Plan ID: " + verify.getId());
        System.out.println("Plan Status: " + verify.getStatus());
        System.out.println("Expected: CANCELLED");
        System.out.println("========================================");
        
        if (verify.getStatus() != MaintenancePlan.PlanStatus.CANCELLED) {
            throw new RuntimeException("CRITICAL: Status was not persisted. Expected CANCELLED, got: " + verify.getStatus());
        }
        
        System.out.println("========================================");
        System.out.println("STATUS PERSISTENCE VERIFIED: CANCELLED");
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
