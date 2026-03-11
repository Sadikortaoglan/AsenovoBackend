package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitElevatorCreateRequest;
import com.saraasansor.api.dto.B2BUnitElevatorListItemResponse;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.dto.ElevatorImportResultResponse;
import com.saraasansor.api.service.ElevatorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitElevatorController {

    private final ElevatorService elevatorService;

    public B2BUnitElevatorController(ElevatorService elevatorService) {
        this.elevatorService = elevatorService;
    }

    @GetMapping("/{id}/elevators")
    public ResponseEntity<ApiResponse<Page<B2BUnitElevatorListItemResponse>>> getElevatorsByB2BUnit(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        Page<B2BUnitElevatorListItemResponse> elevators = elevatorService.getElevatorsByB2BUnit(
                id,
                search,
                PageRequest.of(page, size, parseSort(sort))
        );
        return ResponseEntity.ok(ApiResponse.success(elevators));
    }

    @PostMapping("/{id}/elevators")
    public ResponseEntity<ApiResponse<ElevatorDto>> createElevatorForB2BUnit(
            @PathVariable Long id,
            @Valid @RequestBody B2BUnitElevatorCreateRequest request) {
        ElevatorDto created = elevatorService.createElevatorForB2BUnit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Elevator successfully added", created));
    }

    @PostMapping(value = "/{id}/elevators/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','STAFF_ADMIN','STAFF_USER')")
    public ResponseEntity<ApiResponse<ElevatorImportResultResponse>> importElevatorsForB2BUnit(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ElevatorImportResultResponse result = elevatorService.importFromExcelForB2BUnit(id, file);
        return ResponseEntity.ok(ApiResponse.success("Elevator import completed", result));
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.ASC, "id");
        }

        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElse(Sort.Direction.ASC);
            return Sort.by(direction, parts[0].trim());
        }

        return Sort.by(Sort.Direction.ASC, sort.trim());
    }
}
