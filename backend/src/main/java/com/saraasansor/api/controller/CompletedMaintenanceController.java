package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.MaintenancePlanResponseDto;
import com.saraasansor.api.service.CompletedMaintenanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/maintenance-completions")
public class CompletedMaintenanceController {
    @Autowired
    private CompletedMaintenanceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MaintenancePlanResponseDto>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.listCompleted(from, to, pageable)));
    }
}
