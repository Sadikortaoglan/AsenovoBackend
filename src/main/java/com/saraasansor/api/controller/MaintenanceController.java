package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.MaintenanceDto;
import com.saraasansor.api.dto.MaintenanceSummaryDto;
import com.saraasansor.api.model.FileAttachment;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.FileAttachmentRepository;
import com.saraasansor.api.repository.MaintenanceRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.service.FileStorageService;
import com.saraasansor.api.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/maintenances")
public class MaintenanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);
    
    @Autowired
    private MaintenanceService maintenanceService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;
    
    @Autowired
    private MaintenanceRepository maintenanceRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private com.saraasansor.api.service.ElevatorQrService elevatorQrService;
    
    @Autowired
    private com.saraasansor.api.service.QrSessionService qrSessionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<MaintenanceDto>>> getAllMaintenances(
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        try {
            List<MaintenanceDto> maintenances;
            
            if (paid != null || dateFrom != null || dateTo != null) {
                maintenances = maintenanceService.getMaintenancesByPaidAndDateRange(paid, dateFrom, dateTo);
            } else {
                maintenances = maintenanceService.getAllMaintenances();
            }
            
            return ResponseEntity.ok(ApiResponse.success(maintenances));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MaintenanceSummaryDto>> getMonthlySummary(
            @RequestParam(required = false) String month) {
        try {
            MaintenanceSummaryDto summary = maintenanceService.getMonthlySummary(month);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/elevator/{elevatorId}")
    public ResponseEntity<ApiResponse<List<MaintenanceDto>>> getMaintenancesByElevatorId(
            @PathVariable Long elevatorId) {
        List<MaintenanceDto> maintenances = maintenanceService.getMaintenancesByElevatorId(elevatorId);
        return ResponseEntity.ok(ApiResponse.success(maintenances));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceDto>> getMaintenanceById(@PathVariable Long id) {
        try {
            MaintenanceDto maintenance = maintenanceService.getMaintenanceById(id);
            return ResponseEntity.ok(ApiResponse.success(maintenance));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping(
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<ApiResponse<MaintenanceDto>> createMaintenance(
            @RequestPart(value = "elevatorId", required = true) Long elevatorId,
            @RequestPart(value = "date", required = true) String date,
            @RequestPart(value = "labelType", required = true) String labelType,
            @RequestPart(value = "description", required = true) String description,
            @RequestPart(value = "amount", required = true) String amount, // Accept as String, convert to Double
            @RequestPart(value = "photos", required = true) MultipartFile[] photos,
            @RequestPart(value = "technicianId", required = false) Long technicianId,
            @RequestHeader(value = "X-QR-SESSION-TOKEN", required = false) String qrSessionToken) {
        try {
            // Get current user and role
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("User not authenticated"));
            }
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            User.Role userRole = currentUser.getRole();
            
            logger.debug("Creating maintenance - User: {}, Role: {}, Date: {}, LabelType: {}", 
                currentUser.getUsername(), userRole, date, labelType);
            
            // QR Session Token Validation Guard
            boolean startedRemotely = false;
            
            if (userRole == User.Role.STAFF_USER) {
                // STAFF_USER: QR session token is REQUIRED
                if (qrSessionToken == null || qrSessionToken.trim().isEmpty()) {
                    logger.warn("STAFF_USER attempted to create maintenance without QR session token");
                    return ResponseEntity.status(403)
                        .body(ApiResponse.error("QR validation required. Please scan QR code first."));
                }
                
                // Validate session token
                com.saraasansor.api.service.QrSessionService.TokenValidationResult validation = 
                    qrSessionService.validateToken(qrSessionToken, currentUser.getId(), elevatorId);
                
                if (!validation.isValid()) {
                    logger.warn("Invalid QR session token: {}", validation.getError());
                    return ResponseEntity.status(403)
                        .body(ApiResponse.error("QR validation required: " + validation.getError()));
                }
                
                startedRemotely = validation.isStartedRemotely();
                logger.debug("QR session token validated successfully");
                
                // Update MaintenancePlan status to IN_PROGRESS when QR is validated
                try {
                    LocalDate maintenanceDate = java.time.LocalDate.parse(date);
                    maintenanceService.markPlanAsInProgress(elevatorId, maintenanceDate);
                } catch (Exception e) {
                    logger.warn("Failed to update plan status to IN_PROGRESS: {}", e.getMessage());
                    // Don't fail the request, just log the warning
                }
                
                // Invalidate token after use (one-time use)
                qrSessionService.invalidateToken(qrSessionToken);
                
            } else if (userRole == User.Role.SYSTEM_ADMIN || userRole == User.Role.STAFF_ADMIN) {
                // TODO: Sadık production'da admin QR zorunlu yapmayı isteyebilir. Şimdilik bilinçli olarak açık bırakıldı.
                // Admin roles: QR session token is optional
                if (qrSessionToken != null && !qrSessionToken.trim().isEmpty()) {
                    // Validate session token if provided
                    com.saraasansor.api.service.QrSessionService.TokenValidationResult validation = 
                        qrSessionService.validateToken(qrSessionToken, currentUser.getId(), elevatorId);
                    
                    if (!validation.isValid()) {
                        logger.warn("Invalid QR session token: {}", validation.getError());
                        return ResponseEntity.status(403)
                            .body(ApiResponse.error("Invalid QR session token: " + validation.getError()));
                    }
                    
                    startedRemotely = validation.isStartedRemotely();
                    logger.debug("QR session token validated successfully (optional for admin)");
                    
                    // Update MaintenancePlan status to IN_PROGRESS when QR is validated
                    try {
                        LocalDate maintenanceDate = java.time.LocalDate.parse(date);
                        maintenanceService.markPlanAsInProgress(elevatorId, maintenanceDate);
                    } catch (Exception e) {
                        logger.warn("Failed to update plan status to IN_PROGRESS: {}", e.getMessage());
                        // Don't fail the request, just log the warning
                    }
                    
                    // Invalidate token after use
                    qrSessionService.invalidateToken(qrSessionToken);
                } else {
                    // Admin can create without QR (remote start)
                    startedRemotely = true;
                    logger.debug("Admin creating maintenance without QR (remote start allowed)");
                }
            }
            
            // Log startedRemotely flag (for audit)
            if (startedRemotely) {
                logger.debug("Maintenance created with remote start flag");
            }
            
            // Validation: Minimum 4 photos
            if (photos == null || photos.length < 4) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                        "Maintenance must include at least 4 photos. Provided: " + 
                        (photos != null ? photos.length : 0)
                    ));
            }
            
            // Validate non-empty files
            long validPhotoCount = Arrays.stream(photos)
                .filter(file -> file != null && !file.isEmpty())
                .count();
            
            if (validPhotoCount < 4) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                        "Maintenance must include at least 4 valid photos. Valid photos: " + validPhotoCount
                    ));
            }
            
            // Build MaintenanceDto from request parts
            MaintenanceDto dto = new MaintenanceDto();
            dto.setElevatorId(elevatorId);
            dto.setDate(java.time.LocalDate.parse(date));
            dto.setLabelType(labelType);
            dto.setDescription(description);
            // Parse amount from String to Double
            try {
                dto.setAmount(Double.parseDouble(amount));
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid amount format: " + amount));
            }
            // technicianId is optional - service will auto-assign from SecurityContext
            
            // Create maintenance with photos
            MaintenanceDto created = maintenanceService.createMaintenanceWithPhotos(dto, photos);
            
            logger.info("Maintenance created successfully with {} photos", validPhotoCount);
            
            return ResponseEntity.status(201)
                .body(ApiResponse.success(
                    "Maintenance created successfully with " + validPhotoCount + " photos", 
                    created
                ));
                
        } catch (MaxUploadSizeExceededException e) {
            return ResponseEntity.status(413)
                .body(ApiResponse.error("File size exceeds maximum allowed size of 10MB"));
        } catch (MultipartException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Multipart request processing failed: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Catches MissingServletRequestPartException and other argument exceptions
            if (e.getMessage() != null && e.getMessage().contains("Required part")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Missing required part in multipart request: " + e.getMessage()));
            }
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error in createMaintenance: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceDto>> updateMaintenance(
            @PathVariable Long id, @Valid @RequestBody MaintenanceDto dto) {
        try {
            MaintenanceDto updated = maintenanceService.updateMaintenance(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Maintenance record successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMaintenance(@PathVariable Long id) {
        try {
            maintenanceService.deleteMaintenance(id);
            return ResponseEntity.ok(ApiResponse.success("Maintenance record successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<MaintenanceDto>> markPaid(
            @PathVariable Long id, @RequestParam(defaultValue = "true") boolean paid) {
        try {
            MaintenanceDto updated = maintenanceService.markPaid(id, paid);
            return ResponseEntity.ok(ApiResponse.success(
                paid ? "Marked as paid" : "Payment mark removed", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Upload photos for maintenance
     * Business rule: Maintenance must include at least 4 photos
     */
    @PostMapping("/{id}/photos")
    public ResponseEntity<ApiResponse<String>> uploadPhotos(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files) {
        try {
            // Validate maintenance exists
            if (!maintenanceRepository.existsById(id)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Maintenance record not found"));
            }
            
            // MINIMUM PHOTO VALIDATION: Must include at least 4 photos
            if (files == null || files.length < 4) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Maintenance must include at least 4 photos. Provided: " + 
                            (files != null ? files.length : 0)));
            }
            
            // Get authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User uploadedBy = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String username = userDetails.getUsername();
                uploadedBy = userRepository.findByUsername(username)
                        .orElse(null);
            }
            
            // Save each file
            int savedCount = 0;
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        // Save file to storage
                        String storageKey = fileStorageService.saveFile(file, "MAINTENANCE", id);
                        String url = fileStorageService.getFileUrl(storageKey);
                        
                        // Create FileAttachment record
                        FileAttachment attachment = new FileAttachment();
                        attachment.setEntityType(FileAttachment.EntityType.MAINTENANCE);
                        attachment.setEntityId(id);
                        attachment.setFileName(file.getOriginalFilename());
                        attachment.setContentType(file.getContentType());
                        attachment.setSize(file.getSize());
                        attachment.setStorageKey(storageKey);
                        attachment.setUrl(url);
                        attachment.setUploadedBy(uploadedBy);
                        
                        fileAttachmentRepository.save(attachment);
                        savedCount++;
                    } catch (IOException e) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("Failed to save file: " + file.getOriginalFilename() + " - " + e.getMessage()));
                    }
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                "Successfully uploaded " + savedCount + " photos", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
