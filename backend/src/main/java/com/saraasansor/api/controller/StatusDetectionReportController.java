package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.StatusDetectionReportDto;
import com.saraasansor.api.service.StatusDetectionReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports/status-detections")
public class StatusDetectionReportController {
    @Autowired
    private StatusDetectionReportService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<StatusDetectionReportDto>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.list(startDate, endDate, building, status, pageable)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<StatusDetectionReportDto>> create(
            @Valid @RequestPart("payload") StatusDetectionReportDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.success("Report created", service.create(dto, file)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<StatusDetectionReportDto>> update(
            @PathVariable Long id,
            @RequestPart("payload") StatusDetectionReportDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Report updated", service.update(id, dto, file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Report deleted", null));
    }
}
