package com.saraasansor.api.service;

import com.saraasansor.api.dto.FaultDto;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Fault;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FaultRepository;
import com.saraasansor.api.util.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class FaultService {
    
    @Autowired
    private FaultRepository faultRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private AuditLogger auditLogger;
    
    public List<FaultDto> getAllFaults() {
        return faultRepository.findAllWithElevator().stream()
                .map(FaultDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<FaultDto> getFaultsByStatus(String status) {
        if (status == null) {
            return getAllFaults();
        }
        
        // Map Turkish status values to English enum
        String normalizedStatus = status.toUpperCase();
        if ("ACIK".equals(normalizedStatus)) {
            normalizedStatus = "OPEN";
        } else if ("TAMAMLANDI".equals(normalizedStatus)) {
            normalizedStatus = "COMPLETED";
        }
        
        try {
            Fault.Status statusEnum = Fault.Status.valueOf(normalizedStatus);
            return faultRepository.findByStatusWithElevator(statusEnum).stream()
                    .map(FaultDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status + " (accepted: ACIK/OPEN, TAMAMLANDI/COMPLETED)");
        }
    }
    
    public FaultDto createFault(FaultDto dto) {
        if (dto.getElevatorId() == null) {
            throw new RuntimeException("Elevator ID is required");
        }
        
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        Fault fault = new Fault();
        fault.setElevator(elevator);
        fault.setFaultSubject(dto.getFaultSubject());
        fault.setContactPerson(dto.getContactPerson());
        fault.setBuildingAuthorizedMessage(dto.getBuildingAuthorizedMessage());
        fault.setDescription(dto.getDescription());
        fault.setStatus(Fault.Status.OPEN);
        
        Fault saved = faultRepository.save(fault);
        
        // Reload with elevator to avoid lazy loading issues
        Fault faultWithElevator = faultRepository.findByIdWithElevator(saved.getId())
                .orElse(saved);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("elevatorId", faultWithElevator.getElevator().getId());
        metadata.put("faultSubject", faultWithElevator.getFaultSubject());
        metadata.put("status", faultWithElevator.getStatus().name());
        auditLogger.log("FAULT_CREATED", "FAULT", faultWithElevator.getId(), metadata);
        
        return FaultDto.fromEntity(faultWithElevator);
    }
    
    public FaultDto updateFaultStatus(Long id, String status) {
        Fault fault = faultRepository.findByIdWithElevator(id)
                .orElseThrow(() -> new RuntimeException("Fault record not found"));
        
        Fault.Status oldStatus = fault.getStatus();
        
        // Map Turkish status values to English enum
        String normalizedStatus = status.toUpperCase();
        if ("ACIK".equals(normalizedStatus)) {
            normalizedStatus = "OPEN";
        } else if ("TAMAMLANDI".equals(normalizedStatus)) {
            normalizedStatus = "COMPLETED";
        }
        
        try {
            Fault.Status newStatus = Fault.Status.valueOf(normalizedStatus);
            fault.setStatus(newStatus);
            
            Fault saved = faultRepository.save(fault);
            
            // Reload with elevator to avoid lazy loading issues
            Fault faultWithElevator = faultRepository.findByIdWithElevator(saved.getId())
                    .orElse(saved);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("oldStatus", oldStatus.name());
            metadata.put("newStatus", newStatus.name());
            auditLogger.log("FAULT_STATUS_UPDATED", "FAULT", faultWithElevator.getId(), metadata);
            
            return FaultDto.fromEntity(faultWithElevator);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status + " (accepted: ACIK/OPEN, TAMAMLANDI/COMPLETED)");
        }
    }
    
    public FaultDto getFaultById(Long id) {
        Fault fault = faultRepository.findByIdWithElevator(id)
                .orElseThrow(() -> new RuntimeException("Fault record not found"));
        return FaultDto.fromEntity(fault);
    }
    
    public FaultDto updateFault(Long id, FaultDto dto) {
        Fault fault = faultRepository.findByIdWithElevator(id)
                .orElseThrow(() -> new RuntimeException("Fault record not found"));
        
        if (dto.getElevatorId() == null) {
            throw new RuntimeException("Elevator ID is required");
        }
        
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        fault.setElevator(elevator);
        fault.setFaultSubject(dto.getFaultSubject());
        fault.setContactPerson(dto.getContactPerson());
        fault.setBuildingAuthorizedMessage(dto.getBuildingAuthorizedMessage());
        fault.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            // Map Turkish status values to English enum
            String normalizedStatus = dto.getStatus().toUpperCase();
            if ("ACIK".equals(normalizedStatus)) {
                normalizedStatus = "OPEN";
            } else if ("TAMAMLANDI".equals(normalizedStatus)) {
                normalizedStatus = "COMPLETED";
            }
            
            try {
                fault.setStatus(Fault.Status.valueOf(normalizedStatus));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + dto.getStatus() + " (accepted: ACIK/OPEN, TAMAMLANDI/COMPLETED)");
            }
        }
        
        Fault saved = faultRepository.save(fault);
        
        // Reload with elevator to avoid lazy loading issues
        Fault faultWithElevator = faultRepository.findByIdWithElevator(saved.getId())
                .orElse(saved);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("elevatorId", faultWithElevator.getElevator().getId());
        metadata.put("faultSubject", faultWithElevator.getFaultSubject());
        auditLogger.log("FAULT_UPDATED", "FAULT", faultWithElevator.getId(), metadata);
        
        return FaultDto.fromEntity(faultWithElevator);
    }
    
    public void deleteFault(Long id) {
        Fault fault = faultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fault record not found"));
        
        faultRepository.delete(fault);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("faultSubject", fault.getFaultSubject());
        auditLogger.log("FAULT_DELETED", "FAULT", id, metadata);
    }
}
