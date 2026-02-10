package com.saraasansor.api.service;

import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.model.MaintenanceTemplate;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.MaintenancePlanRepository;
import com.saraasansor.api.repository.MaintenanceTemplateRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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
        return planRepository.findByPlannedDateBetweenOrderByPlannedDateAsc(from, to);
    }
    
    public List<MaintenancePlan> getPlansByElevatorAndDateRange(Long elevatorId, LocalDate from, LocalDate to) {
        return planRepository.findByElevatorIdAndPlannedDateBetweenOrderByPlannedDateAsc(elevatorId, from, to);
    }
    
    public List<MaintenancePlan> getPlansByTechnicianAndDateRange(Long technicianId, LocalDate from, LocalDate to) {
        return planRepository.findByAssignedTechnicianIdAndPlannedDateBetweenOrderByPlannedDateAsc(technicianId, from, to);
    }
    
    public List<MaintenancePlan> getPlansByStatusAndDateRange(MaintenancePlan.PlanStatus status, LocalDate from, LocalDate to) {
        return planRepository.findByPlannedDateBetweenAndStatusOrderByPlannedDateAsc(from, to, status);
    }
    
    public MaintenancePlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance plan not found"));
    }
    
    public MaintenancePlan createPlan(MaintenancePlan plan) {
        // Validate elevator exists
        Elevator elevator = elevatorRepository.findById(plan.getElevator().getId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        plan.setElevator(elevator);
        
        // Validate template exists
        MaintenanceTemplate template = templateRepository.findById(plan.getTemplate().getId())
                .orElseThrow(() -> new RuntimeException("Maintenance template not found"));
        plan.setTemplate(template);
        
        // Validate technician if provided
        if (plan.getAssignedTechnician() != null && plan.getAssignedTechnician().getId() != null) {
            User technician = userRepository.findById(plan.getAssignedTechnician().getId())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            plan.setAssignedTechnician(technician);
        }
        
        if (plan.getStatus() == null) {
            plan.setStatus(MaintenancePlan.PlanStatus.PLANNED);
        }
        
        return planRepository.save(plan);
    }
    
    public List<MaintenancePlan> createBulkPlans(List<MaintenancePlan> plans) {
        return plans.stream()
                .map(this::createPlan)
                .collect(java.util.stream.Collectors.toList());
    }
    
    public MaintenancePlan updatePlan(Long id, MaintenancePlan planData) {
        MaintenancePlan plan = getPlanById(id);
        
        if (planData.getElevator() != null && planData.getElevator().getId() != null) {
            Elevator elevator = elevatorRepository.findById(planData.getElevator().getId())
                    .orElseThrow(() -> new RuntimeException("Elevator not found"));
            plan.setElevator(elevator);
        }
        
        if (planData.getTemplate() != null && planData.getTemplate().getId() != null) {
            MaintenanceTemplate template = templateRepository.findById(planData.getTemplate().getId())
                    .orElseThrow(() -> new RuntimeException("Maintenance template not found"));
            plan.setTemplate(template);
        }
        
        if (planData.getPlannedDate() != null) {
            plan.setPlannedDate(planData.getPlannedDate());
        }
        
        if (planData.getAssignedTechnician() != null) {
            if (planData.getAssignedTechnician().getId() != null) {
                User technician = userRepository.findById(planData.getAssignedTechnician().getId())
                        .orElseThrow(() -> new RuntimeException("Technician not found"));
                plan.setAssignedTechnician(technician);
            } else {
                plan.setAssignedTechnician(null);
            }
        }
        
        if (planData.getStatus() != null) {
            plan.setStatus(planData.getStatus());
        }
        
        return planRepository.save(plan);
    }
    
    public MaintenancePlan cancelPlan(Long id) {
        MaintenancePlan plan = getPlanById(id);
        plan.setStatus(MaintenancePlan.PlanStatus.CANCELLED);
        return planRepository.save(plan);
    }
    
    public void deletePlan(Long id) {
        MaintenancePlan plan = getPlanById(id);
        planRepository.delete(plan);
    }
}
