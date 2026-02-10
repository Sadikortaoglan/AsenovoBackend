package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.dto.ElevatorStatusDto;
import com.saraasansor.api.dto.WarningDto;
import com.saraasansor.api.dto.WarningElevatorDto;
import com.saraasansor.api.dto.WarningGroupDto;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.LabelType;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.util.AuditLogger;
import com.saraasansor.api.util.LabelDurationCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ElevatorService {
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private AuditLogger auditLogger;
    
    public List<ElevatorDto> getAllElevators() {
        return elevatorRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(ElevatorDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public ElevatorDto getElevatorById(Long id) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        ElevatorDto dto = ElevatorDto.fromEntity(elevator);
        
        // Temporary log: Verify manager info is returned
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorService.class);
        log.info("Returning manager info: tc={}, name={}, phone={}", 
            dto.getManagerTcIdentityNo(), 
            dto.getManagerName(), 
            dto.getManagerPhone());
        
        return dto;
    }
    
    public ElevatorDto createElevator(ElevatorDto dto) {
        // VALIDATION: Required fields
        validateElevatorDto(dto);
        
        if (elevatorRepository.existsByIdentityNumber(dto.getIdentityNumber())) {
            throw new RuntimeException("This identity number is already in use");
        }
        
        Elevator elevator = new Elevator();
        mapDtoToEntity(dto, elevator);
        
        // VALIDATION: Label date is required
        if (elevator.getLabelDate() == null) {
            throw new RuntimeException("Label date is required");
        }
        
        // VALIDATION: Label type is required
        if (elevator.getLabelType() == null) {
            throw new RuntimeException("Label type is required");
        }
        
        // VALIDATION: Expiry date is REQUIRED
        if (elevator.getExpiryDate() == null) {
            throw new RuntimeException("End date is required");
        }
        
        // VALIDATION: End date must be after label date
        if (elevator.getLabelDate() != null && !elevator.getExpiryDate().isAfter(elevator.getLabelDate())) {
            throw new RuntimeException("End date must be after label date");
        }
        
        // Calculate status from expiryDate (duration calculation removed - expiryDate must be provided)
        elevator.setStatus(LabelDurationCalculator.calculateStatus(elevator.getExpiryDate()));
        
        Elevator saved = elevatorRepository.save(elevator);
        
        // Log periodic date update
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("labelDate", saved.getLabelDate());
        metadata.put("labelType", saved.getLabelType() != null ? saved.getLabelType().name() : null);
        metadata.put("expiryDate", saved.getExpiryDate());
        metadata.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
        auditLogger.log("ELEVATOR_CREATED", "ELEVATOR", saved.getId(), metadata);
        
        return ElevatorDto.fromEntity(saved);
    }
    
    public ElevatorDto updateElevator(Long id, ElevatorDto dto) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        // Check identityNumber uniqueness if changed
        if (!elevator.getIdentityNumber().equals(dto.getIdentityNumber()) && 
            elevatorRepository.existsByIdentityNumber(dto.getIdentityNumber())) {
            throw new RuntimeException("This identity number is already in use");
        }
        
        LocalDate oldInspectionDate = elevator.getInspectionDate();
        LocalDate oldExpiryDate = elevator.getExpiryDate();
        
        // Log before mapping
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorService.class);
        log.info(
            "Updating elevator: floorCount={}, capacity={}, speed={}, inspectionDate={}, labelType={}, labelDate={}",
            dto.getFloorCount(),
            dto.getCapacity(),
            dto.getSpeed(),
            dto.getInspectionDate(),
            dto.getLabelType(),
            dto.getLabelDate()
        );
        
        // VALIDATION: Required fields
        validateElevatorDto(dto);
        
        mapDtoToEntity(dto, elevator);
        
        // VALIDATION: Label date is required
        if (elevator.getLabelDate() == null) {
            throw new RuntimeException("Label date is required");
        }
        
        // VALIDATION: Label type is required
        if (elevator.getLabelType() == null) {
            throw new RuntimeException("Label type is required");
        }
        
        // VALIDATION: Expiry date is REQUIRED
        if (elevator.getExpiryDate() == null) {
            throw new RuntimeException("End date is required");
        }
        
        // VALIDATION: End date must be after label date
        if (elevator.getLabelDate() != null && !elevator.getExpiryDate().isAfter(elevator.getLabelDate())) {
            throw new RuntimeException("End date must be after label date");
        }
        
        // Calculate status from expiryDate (duration calculation removed - expiryDate must be provided)
        elevator.setStatus(LabelDurationCalculator.calculateStatus(elevator.getExpiryDate()));
        
        Elevator saved = elevatorRepository.save(elevator);
        
        // Log periodic date update if changed
        if (oldInspectionDate == null || !oldInspectionDate.equals(saved.getInspectionDate()) ||
            oldExpiryDate == null || !oldExpiryDate.equals(saved.getExpiryDate())) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("inspectionDate", saved.getInspectionDate());
            metadata.put("expiryDate", saved.getExpiryDate());
            auditLogger.log("PERIODIC_DATE_UPDATED", "ELEVATOR", saved.getId(), metadata);
        }
        
        return ElevatorDto.fromEntity(saved);
    }
    
    public void deleteElevator(Long id) {
        if (!elevatorRepository.existsById(id)) {
            throw new RuntimeException("Elevator not found");
        }
        elevatorRepository.deleteById(id);
    }
    
    public ElevatorStatusDto getElevatorStatus(Long id) {
        Elevator elevator = elevatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        LocalDate now = LocalDate.now();
        LocalDate expiryDate = elevator.getExpiryDate();
        
        ElevatorStatusDto status = new ElevatorStatusDto();
        status.setIdentityNumber(elevator.getIdentityNumber());
        status.setBuildingName(elevator.getBuildingName());
        status.setExpiryDate(expiryDate);
        
        long daysLeft = ChronoUnit.DAYS.between(now, expiryDate);
        status.setDaysLeft(daysLeft);
        
        if (now.isAfter(expiryDate)) {
            status.setStatus("EXPIRED");
        } else if (daysLeft <= 30) {
            status.setStatus("WARNING");
        } else {
            status.setStatus("OK");
        }
        
        return status;
    }
    
    public List<ElevatorDto> getExpiredElevators() {
        return elevatorRepository.findExpiredElevators(LocalDate.now()).stream()
                .map(ElevatorDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<ElevatorDto> getExpiringSoonElevators() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater).stream()
                .map(ElevatorDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get expired elevators as WarningDto with complete information
     * Returns elevators with identityNo, buildingName, address, maintenanceEndDate, and status
     */
    public List<WarningDto> getExpiredElevatorsAsWarnings() {
        LocalDate now = LocalDate.now();
        return elevatorRepository.findExpiredElevators(now).stream()
                .map(elevator -> WarningDto.fromEntity(elevator, "EXPIRED"))
                .collect(Collectors.toList());
    }
    
    /**
     * Get expiring soon elevators as WarningDto with complete information
     * Returns elevators with identityNo, buildingName, address, maintenanceEndDate, and status
     */
    public List<WarningDto> getExpiringSoonElevatorsAsWarnings() {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        return elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater).stream()
                .map(elevator -> WarningDto.fromEntity(elevator, "WARNING"))
                .collect(Collectors.toList());
    }
    
    /**
     * Get warnings grouped by building (buildingName + address)
     * Groups elevators by building and returns structured data for frontend expansion
     * 
     * Sorting:
     * - EXPIRED buildings first
     * - Then WARNING buildings
     * - Within each group, elevators sorted by maintenanceEndDate ASC
     * 
     * @param type Optional: "EXPIRED", "WARNING", or null (both)
     * @return List of WarningGroupDto grouped by building
     */
    public List<WarningGroupDto> getGroupedWarnings(String type) {
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysLater = now.plusDays(30);
        
        List<Elevator> expiredElevators = new ArrayList<>();
        List<Elevator> warningElevators = new ArrayList<>();
        
        if (type == null || "EXPIRED".equalsIgnoreCase(type)) {
            expiredElevators = elevatorRepository.findExpiredElevators(now);
        }
        
        if (type == null || "WARNING".equalsIgnoreCase(type)) {
            warningElevators = elevatorRepository.findExpiringSoonElevators(now, thirtyDaysLater);
        }
        
        // Group expired elevators by building (buildingName + address)
        Map<String, WarningGroupDto> expiredGroups = expiredElevators.stream()
                .collect(Collectors.groupingBy(
                    elevator -> buildGroupKey(elevator.getBuildingName(), elevator.getAddress()),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        elevators -> {
                            Elevator first = elevators.get(0);
                            List<WarningElevatorDto> elevatorDtos = elevators.stream()
                                    .sorted(Comparator.comparing(Elevator::getExpiryDate))
                                    .map(e -> new WarningElevatorDto(
                                        e.getIdentityNumber() != null ? e.getIdentityNumber() : "",
                                        e.getExpiryDate(),
                                        "EXPIRED"
                                    ))
                                    .collect(Collectors.toList());
                            
                            return new WarningGroupDto(
                                first.getBuildingName() != null ? first.getBuildingName() : "",
                                first.getAddress() != null ? first.getAddress() : "",
                                "EXPIRED",
                                elevatorDtos
                            );
                        }
                    )
                ));
        
        // Group warning elevators by building (buildingName + address)
        Map<String, WarningGroupDto> warningGroups = warningElevators.stream()
                .collect(Collectors.groupingBy(
                    elevator -> buildGroupKey(elevator.getBuildingName(), elevator.getAddress()),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        elevators -> {
                            Elevator first = elevators.get(0);
                            List<WarningElevatorDto> elevatorDtos = elevators.stream()
                                    .sorted(Comparator.comparing(Elevator::getExpiryDate))
                                    .map(e -> new WarningElevatorDto(
                                        e.getIdentityNumber() != null ? e.getIdentityNumber() : "",
                                        e.getExpiryDate(),
                                        "WARNING"
                                    ))
                                    .collect(Collectors.toList());
                            
                            return new WarningGroupDto(
                                first.getBuildingName() != null ? first.getBuildingName() : "",
                                first.getAddress() != null ? first.getAddress() : "",
                                "WARNING",
                                elevatorDtos
                            );
                        }
                    )
                ));
        
        // Combine and sort: EXPIRED first, then WARNING
        List<WarningGroupDto> result = new ArrayList<>();
        result.addAll(expiredGroups.values());
        result.addAll(warningGroups.values());
        
        return result;
    }
    
    /**
     * Build a unique key for grouping by buildingName and address
     */
    private String buildGroupKey(String buildingName, String address) {
        String name = buildingName != null ? buildingName : "";
        String addr = address != null ? address : "";
        return name + "|" + addr; // Use pipe separator to combine
    }
    
    private void mapDtoToEntity(ElevatorDto dto, Elevator entity) {
        entity.setIdentityNumber(dto.getIdentityNumber());
        entity.setBuildingName(dto.getBuildingName());
        entity.setAddress(dto.getAddress());
        entity.setElevatorNumber(dto.getElevatorNumber());
        entity.setFloorCount(dto.getFloorCount());
        entity.setCapacity(dto.getCapacity());
        entity.setSpeed(dto.getSpeed());
        entity.setTechnicalNotes(dto.getTechnicalNotes());
        entity.setDriveType(dto.getDriveType());
        entity.setMachineBrand(dto.getMachineBrand());
        entity.setDoorType(dto.getDoorType());
        entity.setInstallationYear(dto.getInstallationYear());
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setControlSystem(dto.getControlSystem());
        entity.setRope(dto.getRope());
        entity.setModernization(dto.getModernization());
        // Map inspectionDate - if not provided, use labelDate as fallback (inspection_date is NOT NULL)
        if (dto.getInspectionDate() != null) {
            entity.setInspectionDate(dto.getInspectionDate());
        } else if (dto.getLabelDate() != null) {
            // Fallback: use labelDate as inspectionDate (inspection_date is NOT NULL)
            entity.setInspectionDate(dto.getLabelDate());
        } else {
            // Both are null - this should not happen due to validation, but set a default
            throw new RuntimeException("Inspection date is required. Please provide inspectionDate or labelDate.");
        }
        
        entity.setBlueLabel(dto.getBlueLabel());
        
        // Map label fields
        if (dto.getLabelDate() != null) {
            entity.setLabelDate(dto.getLabelDate());
        } else if (dto.getInspectionDate() != null) {
            // Fallback: use inspectionDate as labelDate
            entity.setLabelDate(dto.getInspectionDate());
        } else {
            // This should not happen due to validation
            throw new RuntimeException("Label date is required");
        }
        
        // Label type is REQUIRED - parse from DTO
        if (dto.getLabelType() != null && !dto.getLabelType().trim().isEmpty()) {
            try {
                entity.setLabelType(LabelType.valueOf(dto.getLabelType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid label type: " + dto.getLabelType() + ". Valid values: GREEN, YELLOW, RED, ORANGE, BLUE");
            }
        } else {
            // Label type is required - cannot be null
            throw new RuntimeException("Label type is required");
        }
        
        // Map manager fields
        entity.setManagerName(dto.getManagerName());
        entity.setManagerTcIdentityNo(dto.getManagerTcIdentityNo());
        entity.setManagerPhone(dto.getManagerPhone());
        entity.setManagerEmail(dto.getManagerEmail());
        
        // Map expiryDate if provided (otherwise will be calculated from labelDate + duration)
        if (dto.getExpiryDate() != null) {
            entity.setExpiryDate(dto.getExpiryDate());
        }
        
        // Status will be calculated from expiryDate (either provided or calculated)
    }
    
    /**
     * Validate ElevatorDto for required fields and business rules
     */
    private void validateElevatorDto(ElevatorDto dto) {
        // Required fields validation
        if (dto.getIdentityNumber() == null || dto.getIdentityNumber().trim().isEmpty()) {
            throw new RuntimeException("Identity number is required");
        }
        
        if (dto.getBuildingName() == null || dto.getBuildingName().trim().isEmpty()) {
            throw new RuntimeException("Building name is required");
        }
        
        if (dto.getAddress() == null || dto.getAddress().trim().isEmpty()) {
            throw new RuntimeException("Address is required");
        }
        
        if (dto.getLabelType() == null || dto.getLabelType().trim().isEmpty()) {
            throw new RuntimeException("Label type is required");
        }
        
        if (dto.getLabelDate() == null) {
            throw new RuntimeException("Label date is required");
        }
        
        // VALIDATION: Inspection date is REQUIRED (or use labelDate as fallback)
        // Note: inspection_date column is NOT NULL in database
        if (dto.getInspectionDate() == null && dto.getLabelDate() == null) {
            throw new RuntimeException("Inspection date is required. Please provide inspectionDate or labelDate.");
        }
        
        // VALIDATION: Expiry date is REQUIRED (must be provided by frontend)
        if (dto.getExpiryDate() == null) {
            throw new RuntimeException("End date is required");
        }
        
        // VALIDATION: End date must be after label date
        if (dto.getLabelDate() != null && dto.getExpiryDate() != null) {
            if (!dto.getExpiryDate().isAfter(dto.getLabelDate())) {
                throw new RuntimeException("End date must be after label date");
            }
        }
        
        // Manager validation: Name (mandatory)
        if (dto.getManagerName() == null || dto.getManagerName().trim().isEmpty()) {
            throw new RuntimeException("Manager name is required");
        }
        
        // Manager validation: TC Identity Number (mandatory)
        if (dto.getManagerTcIdentityNo() == null || dto.getManagerTcIdentityNo().trim().isEmpty()) {
            throw new RuntimeException("Manager TC Identity Number is required");
        }
        
        // TC Identity Number: Exactly 11 digits, numeric only
        String tcIdentityNo = dto.getManagerTcIdentityNo().trim();
        if (!tcIdentityNo.matches("^[0-9]{11}$")) {
            throw new RuntimeException("Manager TC Identity Number must be exactly 11 digits");
        }
        
        // Manager validation: Phone Number (mandatory)
        // Note: Phone number is already normalized in DTO setter
        if (dto.getManagerPhone() == null || dto.getManagerPhone().trim().isEmpty()) {
            throw new RuntimeException("Manager phone number is required");
        }
        
        // Phone Number: 10 or 11 digits (Turkish format, digits only)
        // Normalization already done in DTO setter (spaces, dashes, parentheses removed)
        String phone = dto.getManagerPhone();
        
        // Temporary debug logging
        org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorService.class);
        log.debug("Validating manager phone number: '{}' (length: {})", phone, phone != null ? phone.length() : 0);
        
        if (!phone.matches("^(0?[0-9]{10})$")) {
            throw new RuntimeException("Phone number must be 10 or 11 digits (Turkish format, digits only)");
        }
        
        // Validate endDate > labelDate
        if (dto.getExpiryDate() != null && dto.getLabelDate() != null) {
            if (!dto.getExpiryDate().isAfter(dto.getLabelDate())) {
                throw new RuntimeException("End date must be after label date");
            }
        }
    }
}
