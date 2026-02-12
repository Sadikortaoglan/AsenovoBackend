package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.dto.ElevatorStatusDto;
import com.saraasansor.api.service.ElevatorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/elevators")
public class ElevatorController {
    
    private static final Logger logger = LoggerFactory.getLogger(ElevatorController.class);
    
    @Autowired
    private ElevatorService elevatorService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ElevatorDto>>> getAllElevators() {
        List<ElevatorDto> elevators = elevatorService.getAllElevators();
        return ResponseEntity.ok(ApiResponse.success(elevators));
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ElevatorStatusDto>> getElevatorStatus(@PathVariable Long id) {
        try {
            ElevatorStatusDto status = elevatorService.getElevatorStatus(id);
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ElevatorDto>> getElevatorById(@PathVariable Long id) {
        try {
            ElevatorDto elevator = elevatorService.getElevatorById(id);
            return ResponseEntity.ok(ApiResponse.success(elevator));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ElevatorDto>> createElevator(@Valid @RequestBody ElevatorDto dto) {
        try {
            // Debug log: Log incoming payload for troubleshooting
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorController.class);
            log.info("Incoming Elevator DTO: identityNumber={}, buildingName={}, labelDate={}, labelType={}, expiryDate={}, managerTcIdentityNo={}, managerPhone={}", 
                dto.getIdentityNumber(), 
                dto.getBuildingName(), 
                dto.getLabelDate(), 
                dto.getLabelType(), 
                dto.getExpiryDate(), 
                dto.getManagerTcIdentityNo(), 
                dto.getManagerPhone());
            
            ElevatorDto created = elevatorService.createElevator(dto);
            return ResponseEntity.ok(ApiResponse.success("Elevator successfully added", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ElevatorDto>> updateElevator(
            @PathVariable Long id, @Valid @RequestBody ElevatorDto dto) {
        try {
            ElevatorDto updated = elevatorService.updateElevator(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Elevator successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteElevator(@PathVariable Long id) {
        try {
            elevatorService.deleteElevator(id);
            return ResponseEntity.ok(ApiResponse.success("Elevator successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @Autowired
    private com.saraasansor.api.service.ElevatorQrService elevatorQrService;
    
    /**
     * GET /api/elevators/{id}/qr
     * Returns QR code image as PNG
     */
    @GetMapping("/{id}/qr")
    public ResponseEntity<?> getElevatorQr(@PathVariable Long id) {
        logger.debug("Generating QR image for elevator ID: {}", id);
        try {
            // Check if service is injected
            if (elevatorQrService == null) {
                logger.error("elevatorQrService is NULL");
                return ResponseEntity.status(500)
                        .header("Content-Type", "application/json")
                        .body(ApiResponse.error("QR service not available"));
            }
            
            byte[] qrImageBytes = elevatorQrService.generateQrImagePng(id);
            
            if (qrImageBytes == null || qrImageBytes.length == 0) {
                logger.error("QR image bytes is null or empty");
                return ResponseEntity.status(500)
                        .header("Content-Type", "application/json")
                        .body(ApiResponse.error("Failed to generate QR code: Empty image"));
            }
            
            logger.debug("QR image generated successfully, size: {} bytes", qrImageBytes.length);
            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Content-Disposition", "inline; filename=\"elevator-" + id + "-qr.png\"")
                    .body(qrImageBytes);
        } catch (Exception e) {
            logger.error("Error in getElevatorQr for id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body(ApiResponse.error("Failed to generate QR code: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/elevators/{id}/qr/download?format=png|pdf
     * Downloads QR code as PNG or PDF
     */
    @GetMapping("/{id}/qr/download")
    public ResponseEntity<?> downloadElevatorQr(
            @PathVariable Long id,
            @RequestParam(defaultValue = "png") String format) {
        try {
            byte[] fileBytes;
            String contentType;
            String filename;
            
            if ("pdf".equalsIgnoreCase(format)) {
                fileBytes = elevatorQrService.generateQrPdf(id);
                contentType = "application/pdf";
                filename = "elevator-" + id + "-qr.pdf";
            } else {
                fileBytes = elevatorQrService.generateQrImagePng(id);
                contentType = "image/png";
                filename = "elevator-" + id + "-qr.png";
            }
            
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(fileBytes);
        } catch (Exception e) {
            logger.error("Error in downloadElevatorQr for id={}, format={}: {}", id, format, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .header("Content-Type", "application/json")
                    .body(ApiResponse.error("Failed to generate QR code: " + e.getMessage()));
        }
    }
}

