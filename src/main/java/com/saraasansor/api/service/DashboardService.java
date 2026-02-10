package com.saraasansor.api.service;

import com.saraasansor.api.dto.CountsDto;
import com.saraasansor.api.dto.DashboardSummaryDto;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.model.MaintenanceSession;
import com.saraasansor.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DashboardService {
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private MaintenanceRepository maintenanceRepository;
    
    @Autowired
    private MaintenanceService maintenanceService;
    
    @Autowired
    private com.saraasansor.api.repository.InspectionRepository inspectionRepository;
    
    @Autowired
    private com.saraasansor.api.repository.FaultRepository faultRepository;
    
    @Autowired
    private com.saraasansor.api.repository.PartRepository partRepository;
    
    @Autowired
    private MaintenanceTemplateRepository maintenanceTemplateRepository;
    
    @Autowired
    private MaintenancePlanRepository maintenancePlanRepository;
    
    @Autowired
    private MaintenanceSessionRepository maintenanceSessionRepository;
    
    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();
        
        summary.setTotalElevators(elevatorRepository.count());
        summary.setTotalMaintenances(maintenanceRepository.count());
        
        // Calculate total income (paid maintenances)
        Double totalIncome = maintenanceRepository.findAll().stream()
                .filter(m -> m.getIsPaid() != null && m.getIsPaid() && m.getAmount() != null)
                .mapToDouble(m -> m.getAmount())
                .sum();
        summary.setTotalIncome(totalIncome);
        
        // Calculate total debt (unpaid maintenances)
        Double totalDebt = maintenanceRepository.findAll().stream()
                .filter(m -> m.getIsPaid() == null || !m.getIsPaid())
                .filter(m -> m.getAmount() != null)
                .mapToDouble(m -> m.getAmount())
                .sum();
        summary.setTotalDebt(totalDebt);
        
        // Count expired elevators
        summary.setExpiredCount((long) elevatorRepository.findExpiredElevators(LocalDate.now()).size());
        
        // Count warning elevators
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        summary.setWarningCount((long) elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater).size());
        
        return summary;
    }
    
    /**
     * Get counts for sidebar badges
     */
    public CountsDto getCounts() {
        CountsDto counts = new CountsDto();
        
        // Basic counts
        counts.setElevators(elevatorRepository.count());
        counts.setMaintenances(maintenanceRepository.count());
        counts.setInspections(inspectionRepository.count());
        counts.setFaults(faultRepository.count());
        counts.setParts(partRepository.count());
        
        // Warnings (expired + warning)
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        long expiredCount = elevatorRepository.findExpiredElevators(now).size();
        long warningCount = elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater).size();
        counts.setWarnings(expiredCount + warningCount);
        
        // Maintenance module counts
        counts.setMaintenanceTemplates(maintenanceTemplateRepository.count());
        
        // Upcoming plans (PLANNED status, plannedDate >= today)
        long upcomingPlans = maintenancePlanRepository.findByPlannedDateBetweenAndStatusOrderByPlannedDateAsc(
                now, now.plusYears(1), MaintenancePlan.PlanStatus.PLANNED).size();
        counts.setMaintenancePlansUpcoming(upcomingPlans);
        
        // Completed sessions (last 30 days)
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        long completedSessions = maintenanceSessionRepository.findByStatusAndStartAtBetween(
                MaintenanceSession.SessionStatus.COMPLETED, from, to, 
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        counts.setMaintenanceSessionsCompleted(completedSessions);
        
        return counts;
    }
}
