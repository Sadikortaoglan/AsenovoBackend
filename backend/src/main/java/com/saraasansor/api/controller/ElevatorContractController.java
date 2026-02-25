package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorContractDto;
import com.saraasansor.api.service.ElevatorContractService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/elevator-contracts")
public class ElevatorContractController {
    @Autowired
    private ElevatorContractService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ElevatorContractDto>>> list(
            @RequestParam(required = false) Long elevatorId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.list(elevatorId, pageable)));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ElevatorContractDto>> create(
            @Valid @RequestPart("payload") ElevatorContractDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(201).body(ApiResponse.success("Elevator contract created", service.create(dto, file)));
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ElevatorContractDto>> update(
            @PathVariable Long id,
            @RequestPart("payload") ElevatorContractDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Elevator contract updated", service.update(id, dto, file)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Elevator contract deleted", null));
    }
}
