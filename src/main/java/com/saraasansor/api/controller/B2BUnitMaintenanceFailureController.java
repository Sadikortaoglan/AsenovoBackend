package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitMaintenanceFailureListItemResponse;
import com.saraasansor.api.service.B2BUnitMaintenanceFailureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b2b-units")
public class B2BUnitMaintenanceFailureController {

    private final B2BUnitMaintenanceFailureService maintenanceFailureService;

    public B2BUnitMaintenanceFailureController(B2BUnitMaintenanceFailureService maintenanceFailureService) {
        this.maintenanceFailureService = maintenanceFailureService;
    }

    @GetMapping("/{id}/maintenance-failures")
    public ResponseEntity<ApiResponse<Page<B2BUnitMaintenanceFailureListItemResponse>>> getCompletedMaintenanceFailures(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "operationDate,desc") String sort) {

        Page<B2BUnitMaintenanceFailureListItemResponse> result = maintenanceFailureService.getCompletedMaintenanceFailures(
                id,
                search,
                PageRequest.of(page, size, parseSort(sort))
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private Sort parseSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "operationDate");
        }

        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElse(Sort.Direction.DESC);
            return Sort.by(direction, parts[0].trim());
        }

        return Sort.by(Sort.Direction.DESC, sort.trim());
    }
}
