package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorLabelDto;
import com.saraasansor.api.service.ElevatorLabelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/elevator-labels")
public class ElevatorLabelController {
    @Autowired
    private ElevatorLabelService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ElevatorLabelDto>>> list(
            @RequestParam(required = false) Long elevatorId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.list(elevatorId, pageable)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ElevatorLabelDto>> create(
            @Valid @RequestPart("payload") ElevatorLabelDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.success("Elevator label created", service.create(dto, file)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ElevatorLabelDto>> update(
            @PathVariable Long id,
            @RequestPart("payload") ElevatorLabelDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Elevator label updated", service.update(id, dto, file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Elevator label deleted", null));
    }
}
