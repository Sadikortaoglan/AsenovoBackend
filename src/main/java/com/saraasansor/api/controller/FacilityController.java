package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.CreateFacilityRequest;
import com.saraasansor.api.dto.FacilityAddressDto;
import com.saraasansor.api.dto.FacilityDto;
import com.saraasansor.api.dto.FacilityImportResultDto;
import com.saraasansor.api.dto.FacilityMovementDto;
import com.saraasansor.api.dto.UpdateFacilityRequest;
import com.saraasansor.api.model.Facility;
import com.saraasansor.api.service.FacilityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FacilityDto>>> getFacilities(
            @RequestParam(required = false) String query,
            @RequestParam(name = "b2b_unit_id", required = false) Long b2bUnitId,
            @RequestParam(name = "b2bUnitId", required = false) Long legacyB2bUnitId,
            @RequestParam(required = false) Facility.FacilityStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        Long effectiveB2bUnitId = b2bUnitId != null ? b2bUnitId : legacyB2bUnitId;

        Page<FacilityDto> facilities = facilityService.getFacilities(
                query,
                effectiveB2bUnitId,
                status,
                PageRequest.of(page, size, parseSort(sort))
        );
        return ResponseEntity.ok(ApiResponse.success(facilities));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityDto>> getFacilityById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getFacilityById(id)));
    }

    @GetMapping("/{id}/address")
    public ResponseEntity<ApiResponse<FacilityAddressDto>> getFacilityAddress(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getFacilityAddressById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FacilityDto>> createFacility(@Valid @RequestBody CreateFacilityRequest request) {
        FacilityDto created = facilityService.createFacility(request);
        return ResponseEntity.ok(ApiResponse.success("Facility successfully created", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FacilityDto>> updateFacility(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFacilityRequest request) {
        FacilityDto updated = facilityService.updateFacility(id, request);
        return ResponseEntity.ok(ApiResponse.success("Facility successfully updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return ResponseEntity.ok(ApiResponse.success("Facility successfully deactivated (soft delete)", null));
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<ApiResponse<FacilityMovementDto>> getFacilityMovements(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(facilityService.getFacilityMovements(id)));
    }

    @GetMapping(value = "/{id}/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getFacilityReport(@PathVariable Long id) {
        return ResponseEntity.ok(facilityService.buildFacilityReportHtml(id));
    }

    @PostMapping(value = "/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FacilityImportResultDto>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "createMissingB2BUnit", defaultValue = "true") boolean createMissingB2BUnit) {
        FacilityImportResultDto result = facilityService.importFromExcel(file, createMissingB2BUnit);
        return ResponseEntity.ok(ApiResponse.success("Facility import completed", result));
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
