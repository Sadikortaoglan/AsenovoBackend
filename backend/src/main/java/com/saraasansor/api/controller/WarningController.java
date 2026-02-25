package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.WarningDto;
import com.saraasansor.api.dto.WarningGroupDto;
import com.saraasansor.api.service.ElevatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/warnings")
public class WarningController {
    
    @Autowired
    private ElevatorService elevatorService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<WarningDto>>> getWarnings(
            @RequestParam(required = false) String type) {
        List<WarningDto> warnings = new ArrayList<>();
        
        if ("EXPIRED".equalsIgnoreCase(type)) {
            warnings = elevatorService.getExpiredElevatorsAsWarnings();
        } else if ("WARNING".equalsIgnoreCase(type)) {
            warnings = elevatorService.getExpiringSoonElevatorsAsWarnings();
        } else {
            // Return both
            warnings = elevatorService.getExpiredElevatorsAsWarnings();
            warnings.addAll(elevatorService.getExpiringSoonElevatorsAsWarnings());
        }
        
        return ResponseEntity.ok(ApiResponse.success(warnings));
    }
    
    /**
     * Get warnings grouped by building (buildingName + address)
     * Returns structured data for frontend expansion UI
     * 
     * @param type Optional: "EXPIRED", "WARNING", or null (both)
     * @return List of WarningGroupDto grouped by building
     */
    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<List<WarningGroupDto>>> getGroupedWarnings(
            @RequestParam(required = false) String type) {
        List<WarningGroupDto> groupedWarnings = elevatorService.getGroupedWarnings(type);
        return ResponseEntity.ok(ApiResponse.success(groupedWarnings));
    }
}

