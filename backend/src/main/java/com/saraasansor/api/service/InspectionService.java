package com.saraasansor.api.service;

import com.saraasansor.api.dto.InspectionDto;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.Inspection;
import com.saraasansor.api.model.InspectionColor;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.InspectionRepository;
import com.saraasansor.api.util.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class InspectionService {
    
    @Autowired
    private InspectionRepository inspectionRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private AuditLogger auditLogger;
    
    public List<InspectionDto> getAllInspections() {
        // Sort by date DESC (newest first), then by id DESC for consistent ordering
        return inspectionRepository.findAllWithElevator().stream()
                .map(InspectionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<InspectionDto> getInspectionsByElevatorId(Long elevatorId) {
        return inspectionRepository.findByElevatorIdWithElevator(elevatorId).stream()
                .map(InspectionDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public InspectionDto createInspection(InspectionDto dto) {
        // VALIDATION: Inspection color is REQUIRED
        if (dto.getInspectionColor() == null || dto.getInspectionColor().trim().isEmpty()) {
            throw new RuntimeException("Inspection color is required");
        }
        
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        Inspection inspection = new Inspection();
        inspection.setElevator(elevator);
        inspection.setDate(dto.getDate());
        inspection.setResult(dto.getResult());
        
        // Parse inspection color from string to enum
        try {
            inspection.setInspectionColor(InspectionColor.valueOf(dto.getInspectionColor().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid inspection color: " + dto.getInspectionColor() + ". Valid values: GREEN, YELLOW, RED, ORANGE");
        }
        
        inspection.setContactedPersonName(dto.getContactedPersonName());
        inspection.setDescription(dto.getDescription());
        
        Inspection saved = inspectionRepository.save(inspection);
        
        // Reload with elevator to avoid lazy loading issues
        Inspection inspectionWithElevator = inspectionRepository.findByIdWithElevator(saved.getId())
                .orElse(saved);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("elevatorId", inspectionWithElevator.getElevator().getId());
        metadata.put("date", inspectionWithElevator.getDate());
        metadata.put("result", inspectionWithElevator.getResult());
        metadata.put("inspectionColor", inspectionWithElevator.getInspectionColor() != null ? inspectionWithElevator.getInspectionColor().name() : null);
        metadata.put("contactedPersonName", inspectionWithElevator.getContactedPersonName());
        auditLogger.log("INSPECTION_CREATED", "INSPECTION", inspectionWithElevator.getId(), metadata);
        
        return InspectionDto.fromEntity(inspectionWithElevator);
    }
    
    public InspectionDto getInspectionById(Long id) {
        Inspection inspection = inspectionRepository.findByIdWithElevator(id)
                .orElseThrow(() -> new RuntimeException("Inspection record not found"));
        return InspectionDto.fromEntity(inspection);
    }
    
    public InspectionDto updateInspection(Long id, InspectionDto dto) {
        // VALIDATION: Inspection color is REQUIRED
        if (dto.getInspectionColor() == null || dto.getInspectionColor().trim().isEmpty()) {
            throw new RuntimeException("Inspection color is required");
        }
        
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inspection record not found"));
        
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        inspection.setElevator(elevator);
        inspection.setDate(dto.getDate());
        inspection.setResult(dto.getResult());
        
        // Parse inspection color from string to enum
        try {
            inspection.setInspectionColor(InspectionColor.valueOf(dto.getInspectionColor().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid inspection color: " + dto.getInspectionColor() + ". Valid values: GREEN, YELLOW, RED, ORANGE");
        }
        
        inspection.setContactedPersonName(dto.getContactedPersonName());
        inspection.setDescription(dto.getDescription());
        
        Inspection saved = inspectionRepository.save(inspection);
        
        // Reload with elevator to avoid lazy loading issues
        Inspection inspectionWithElevator = inspectionRepository.findByIdWithElevator(saved.getId())
                .orElse(saved);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("elevatorId", inspectionWithElevator.getElevator().getId());
        metadata.put("date", inspectionWithElevator.getDate());
        metadata.put("result", inspectionWithElevator.getResult());
        metadata.put("inspectionColor", inspectionWithElevator.getInspectionColor() != null ? inspectionWithElevator.getInspectionColor().name() : null);
        metadata.put("contactedPersonName", inspectionWithElevator.getContactedPersonName());
        auditLogger.log("INSPECTION_UPDATED", "INSPECTION", inspectionWithElevator.getId(), metadata);
        
        return InspectionDto.fromEntity(inspectionWithElevator);
    }
    
    public void deleteInspection(Long id) {
        Inspection inspection = inspectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inspection record not found"));
        
        inspectionRepository.delete(inspection);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("date", inspection.getDate());
        metadata.put("result", inspection.getResult());
        auditLogger.log("INSPECTION_DELETED", "INSPECTION", id, metadata);
    }
}
