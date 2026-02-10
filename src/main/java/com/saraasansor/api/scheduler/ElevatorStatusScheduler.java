package com.saraasansor.api.scheduler;

import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.util.LabelDurationCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to recalculate elevator statuses based on end_date
 * Runs daily at 3:00 AM
 */
@Component
public class ElevatorStatusScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(ElevatorStatusScheduler.class);
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    /**
     * Recalculate elevator statuses based on end_date
     * Business rule: if (today > endDate) → EXPIRED, else → ACTIVE
     * 
     * Runs daily at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * *") // Every day at 3:00 AM
    @Transactional
    public void recalculateElevatorStatuses() {
        log.info("Starting elevator status recalculation job");
        
        LocalDate today = LocalDate.now();
        List<Elevator> elevators = elevatorRepository.findAll();
        
        int updatedCount = 0;
        for (Elevator elevator : elevators) {
            if (elevator.getExpiryDate() == null) {
                continue;
            }
            
            // Calculate status based on end_date
            Elevator.Status newStatus = LabelDurationCalculator.calculateStatus(elevator.getExpiryDate());
            
            // Only update if status changed
            if (elevator.getStatus() != newStatus) {
                elevator.setStatus(newStatus);
                elevatorRepository.save(elevator);
                updatedCount++;
                log.debug("Updated elevator {} status from {} to {}", 
                    elevator.getId(), elevator.getStatus(), newStatus);
            }
        }
        
        log.info("Elevator status recalculation completed. Updated {} elevators", updatedCount);
    }
    
    /**
     * Recalculate end_date and status for elevators where labelDate or labelType changed
     * This is a safety net in case manual updates bypassed the service layer
     * 
     * Runs daily at 3:30 AM (30 minutes after status update)
     */
    @Scheduled(cron = "0 30 3 * * *") // Every day at 3:30 AM
    @Transactional
    public void recalculateEndDates() {
        log.info("Starting elevator end date recalculation job");
        
        List<Elevator> elevators = elevatorRepository.findAll();
        
        int updatedCount = 0;
        for (Elevator elevator : elevators) {
            if (elevator.getLabelDate() == null || elevator.getLabelType() == null) {
                continue;
            }
            
            // Recalculate endDate from labelDate + labelDuration
            LocalDate calculatedEndDate = LabelDurationCalculator.calculateEndDate(
                elevator.getLabelDate(),
                elevator.getLabelType()
            );
            
            // Update if different
            if (!calculatedEndDate.equals(elevator.getExpiryDate())) {
                elevator.setExpiryDate(calculatedEndDate);
                elevator.setStatus(LabelDurationCalculator.calculateStatus(calculatedEndDate));
                elevatorRepository.save(elevator);
                updatedCount++;
                log.debug("Updated elevator {} endDate from {} to {}", 
                    elevator.getId(), elevator.getExpiryDate(), calculatedEndDate);
            }
        }
        
        log.info("Elevator end date recalculation completed. Updated {} elevators", updatedCount);
    }
}
