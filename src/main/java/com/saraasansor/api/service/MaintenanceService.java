package com.saraasansor.api.service;

import com.saraasansor.api.dto.MaintenanceDto;
import com.saraasansor.api.dto.MaintenanceSummaryDto;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.FileAttachment;
import com.saraasansor.api.model.LabelType;
import com.saraasansor.api.model.Maintenance;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.FileAttachmentRepository;
import com.saraasansor.api.repository.MaintenanceRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.service.FileStorageService;
import com.saraasansor.api.util.LabelDurationCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenanceService {
    
    @Autowired
    private MaintenanceRepository maintenanceRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;
    
    public List<MaintenanceDto> getAllMaintenances() {
        return maintenanceRepository.findAll().stream()
                .map(MaintenanceDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public MaintenanceDto getMaintenanceById(Long id) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));
        return MaintenanceDto.fromEntity(maintenance);
    }
    
    public List<MaintenanceDto> getMaintenancesByElevatorId(Long elevatorId) {
        return maintenanceRepository.findByElevatorIdOrderByDateDesc(elevatorId).stream()
                .map(MaintenanceDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public MaintenanceDto createMaintenance(MaintenanceDto dto) {
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        Maintenance maintenance = new Maintenance();
        maintenance.setElevator(elevator);
        maintenance.setDate(dto.getDate());
        
        // Set labelType from DTO
        if (dto.getLabelType() != null) {
            try {
                maintenance.setLabelType(LabelType.valueOf(dto.getLabelType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                maintenance.setLabelType(LabelType.BLUE); // Default
            }
        } else {
            maintenance.setLabelType(LabelType.BLUE); // Default
        }
        
        maintenance.setDescription(dto.getDescription());
        maintenance.setAmount(dto.getAmount());
        maintenance.setIsPaid(dto.getIsPaid() != null ? dto.getIsPaid() : false);
        maintenance.setPaymentDate(dto.getPaymentDate());
        
        // AUTO-ASSIGN TECHNICIAN: Get from SecurityContext (logged-in user)
        // Business rule: Technician must be set automatically as the currently authenticated user
        // Do NOT accept technicianId or technicianName from request body
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            User technician = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));
            maintenance.setTechnician(technician);
        } else {
            throw new RuntimeException("User not authenticated. Cannot assign technician.");
        }
        
        Maintenance saved = maintenanceRepository.save(maintenance);
        
        // AUTO-UPDATE ELEVATOR: Update elevator's labelType, labelDate, endDate, and status
        // Business rule: When maintenance is created with a label, update elevator automatically
        elevator.setLabelType(saved.getLabelType());
        elevator.setLabelDate(saved.getDate()); // Use maintenance date as new label date
        
        // Recalculate endDate and status
        LabelDurationCalculator.StatusResult result = LabelDurationCalculator.calculateStatusAndEndDate(
            elevator.getLabelDate(), 
            elevator.getLabelType()
        );
        elevator.setExpiryDate(result.getEndDate());
        elevator.setStatus(result.getStatus());
        
        elevatorRepository.save(elevator);
        
        return MaintenanceDto.fromEntity(saved);
    }
    
    @Transactional
    public MaintenanceDto createMaintenanceWithPhotos(MaintenanceDto dto, MultipartFile[] photos) {
        // 1. Create maintenance record (reuse existing logic)
        MaintenanceDto created = createMaintenance(dto);
        
        // 2. Get authenticated user for file attachments
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User uploadedBy = null;
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();
            uploadedBy = userRepository.findByUsername(username).orElse(null);
        }
        
        // 3. Save photos (atomic transaction - if any fails, entire operation rolls back)
        for (MultipartFile photo : photos) {
            if (photo != null && !photo.isEmpty()) {
                try {
                    // Save file to storage
                    String storageKey = fileStorageService.saveFile(photo, "MAINTENANCE", created.getId());
                    String url = fileStorageService.getFileUrl(storageKey);
                    
                    // Create FileAttachment record
                    FileAttachment attachment = new FileAttachment();
                    attachment.setEntityType(FileAttachment.EntityType.MAINTENANCE);
                    attachment.setEntityId(created.getId());
                    attachment.setFileName(photo.getOriginalFilename());
                    attachment.setContentType(photo.getContentType());
                    attachment.setSize(photo.getSize());
                    attachment.setStorageKey(storageKey);
                    attachment.setUrl(url);
                    attachment.setUploadedBy(uploadedBy);
                    
                    fileAttachmentRepository.save(attachment);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save photo: " + photo.getOriginalFilename() + " - " + e.getMessage());
                }
            }
        }
        
        // 4. Return created maintenance (photos are linked via FileAttachment records)
        return created;
    }
    
    public MaintenanceDto updateMaintenance(Long id, MaintenanceDto dto) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));
        
        maintenance.setDate(dto.getDate());
        maintenance.setDescription(dto.getDescription());
        maintenance.setAmount(dto.getAmount());
        maintenance.setIsPaid(dto.getIsPaid());
        maintenance.setPaymentDate(dto.getPaymentDate());
        
        // Business rule: Technician must NOT be changeable via API
        // Do NOT update technician - it remains as originally assigned during creation
        
        Maintenance saved = maintenanceRepository.save(maintenance);
        return MaintenanceDto.fromEntity(saved);
    }
    
    public void deleteMaintenance(Long id) {
        if (!maintenanceRepository.existsById(id)) {
            throw new RuntimeException("Maintenance record not found");
        }
        maintenanceRepository.deleteById(id);
    }
    
    public MaintenanceDto markPaid(Long id, boolean paid) {
        Maintenance maintenance = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));
        
        maintenance.setIsPaid(paid);
        if (paid) {
            maintenance.setPaymentDate(LocalDate.now());
        } else {
            maintenance.setPaymentDate(null);
        }
        
        Maintenance saved = maintenanceRepository.save(maintenance);
        return MaintenanceDto.fromEntity(saved);
    }
    
    public List<MaintenanceDto> getMaintenancesByPaidStatus(Boolean paid) {
        if (paid == null) {
            return getAllMaintenances();
        }
        return maintenanceRepository.findByIsPaidOrderByDateDesc(paid).stream()
                .map(MaintenanceDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<MaintenanceDto> getMaintenancesByDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return getAllMaintenances();
        }
        
        LocalDate start = dateFrom != null ? dateFrom : LocalDate.of(1900, 1, 1);
        LocalDate end = dateTo != null ? dateTo : LocalDate.now();
        
        return maintenanceRepository.findByDateBetween(start, end).stream()
                .map(MaintenanceDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<MaintenanceDto> getMaintenancesByPaidAndDateRange(Boolean paid, LocalDate dateFrom, LocalDate dateTo) {
        if (paid == null && dateFrom == null && dateTo == null) {
            return getAllMaintenances();
        }
        
        if (paid == null) {
            return getMaintenancesByDateRange(dateFrom, dateTo);
        }
        
        LocalDate start = dateFrom != null ? dateFrom : LocalDate.of(1900, 1, 1);
        LocalDate end = dateTo != null ? dateTo : LocalDate.now();
        
        return maintenanceRepository.findByIsPaidAndDateBetween(paid, start, end).stream()
                .map(MaintenanceDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public MaintenanceSummaryDto getMonthlySummary(String month) {
        // month format: YYYY-MM
        LocalDate monthDate;
        try {
            if (month == null || month.isEmpty()) {
                monthDate = LocalDate.now();
            } else {
                monthDate = LocalDate.parse(month + "-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid date format. Must be in YYYY-MM format.");
        }
        
        int year = monthDate.getYear();
        int monthValue = monthDate.getMonthValue();
        
        List<Maintenance> maintenances = maintenanceRepository.findByYearAndMonth(year, monthValue);
        
        MaintenanceSummaryDto summary = new MaintenanceSummaryDto();
        summary.setTotalCount(maintenances.size());
        
        long paidCount = maintenances.stream().filter(m -> Boolean.TRUE.equals(m.getIsPaid())).count();
        summary.setPaidCount((int) paidCount);
        summary.setUnpaidCount((int) (maintenances.size() - paidCount));
        
        double totalAmount = maintenances.stream()
                .filter(m -> m.getAmount() != null)
                .mapToDouble(Maintenance::getAmount)
                .sum();
        summary.setTotalAmount(totalAmount);
        
        double paidAmount = maintenances.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsPaid()) && m.getAmount() != null)
                .mapToDouble(Maintenance::getAmount)
                .sum();
        summary.setPaidAmount(paidAmount);
        
        summary.setUnpaidAmount(totalAmount - paidAmount);
        
        return summary;
    }
}
