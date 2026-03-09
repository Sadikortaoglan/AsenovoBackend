package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitFacilityCreateRequest;
import com.saraasansor.api.dto.FacilityDto;
import com.saraasansor.api.service.FacilityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitFacilityController {

    private final FacilityService facilityService;

    public B2BUnitFacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping("/{id}/facilities")
    public ResponseEntity<ApiResponse<Page<FacilityDto>>> getFacilitiesByB2BUnit(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        Page<FacilityDto> facilities = facilityService.getFacilitiesByB2BUnit(
                id,
                search,
                PageRequest.of(page, size, parseSort(sort))
        );
        return ResponseEntity.ok(ApiResponse.success(facilities));
    }

    @PostMapping("/{id}/facilities")
    public ResponseEntity<ApiResponse<FacilityDto>> createFacilityForB2BUnit(
            @PathVariable Long id,
            @Valid @RequestBody B2BUnitFacilityCreateRequest request) {
        FacilityDto created = facilityService.createFacilityForB2BUnit(id, request);
        return ResponseEntity.ok(ApiResponse.success("Facility successfully created", created));
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
