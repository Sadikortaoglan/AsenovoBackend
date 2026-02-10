package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CountsDto;
import com.saraasansor.api.dto.DashboardSummaryDto;
import com.saraasansor.api.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary() {
        DashboardSummaryDto summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    @GetMapping("/counts")
    public ResponseEntity<ApiResponse<CountsDto>> getCounts() {
        CountsDto counts = dashboardService.getCounts();
        return ResponseEntity.ok(ApiResponse.success(counts));
    }
}

