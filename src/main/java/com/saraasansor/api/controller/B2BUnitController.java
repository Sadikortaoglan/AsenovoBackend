package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitLookupDto;
import com.saraasansor.api.dto.B2BUnitDto;
import com.saraasansor.api.dto.CreateB2BUnitRequest;
import com.saraasansor.api.dto.UpdateB2BUnitRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.service.B2BUnitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/b2bunits")
public class B2BUnitController {

    private final B2BUnitService b2bUnitService;

    public B2BUnitController(B2BUnitService b2bUnitService) {
        this.b2bUnitService = b2bUnitService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<B2BUnitDto>>> getAllB2BUnits(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {

        Page<B2BUnit> units = b2bUnitService.getB2BUnits(query, PageRequest.of(page, size, parseSort(sort)));
        Page<B2BUnitDto> response = units.map(B2BUnitDto::fromEntity);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<B2BUnitDto>> getB2BUnitById(@PathVariable Long id) {
        B2BUnit unit = b2bUnitService.getB2BUnitById(id);
        return ResponseEntity.ok(ApiResponse.success(B2BUnitDto.fromEntity(unit)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<B2BUnitDto>> getMyB2BUnit() {
        B2BUnit unit = b2bUnitService.getMyB2BUnit();
        return ResponseEntity.ok(ApiResponse.success(B2BUnitDto.fromEntity(unit)));
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<List<B2BUnitLookupDto>>> lookupB2BUnits(
            @RequestParam(required = false) String query) {
        List<B2BUnitLookupDto> result = b2bUnitService.getLookup(query).stream()
                .map(B2BUnitLookupDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<B2BUnitDto>> createB2BUnit(@Valid @RequestBody CreateB2BUnitRequest request) {
        B2BUnit created = b2bUnitService.createB2BUnit(request);
        return ResponseEntity.ok(ApiResponse.success("B2B unit successfully created", B2BUnitDto.fromEntity(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<B2BUnitDto>> updateB2BUnit(
            @PathVariable Long id,
            @Valid @RequestBody UpdateB2BUnitRequest request) {
        B2BUnit updated = b2bUnitService.updateB2BUnit(id, request);
        return ResponseEntity.ok(ApiResponse.success("B2B unit successfully updated", B2BUnitDto.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteB2BUnit(@PathVariable Long id) {
        b2bUnitService.deleteB2BUnit(id);
        return ResponseEntity.ok(ApiResponse.success("B2B unit successfully deactivated (soft delete)", null));
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
