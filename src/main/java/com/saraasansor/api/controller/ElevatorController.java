package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.dto.ElevatorStatusDto;
import com.saraasansor.api.service.ElevatorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/elevators")
public class ElevatorController {
    
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
}

