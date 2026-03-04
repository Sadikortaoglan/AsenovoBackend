package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.B2BUnitGroupDto;
import com.saraasansor.api.dto.CreateB2BUnitGroupRequest;
import com.saraasansor.api.dto.UpdateB2BUnitGroupRequest;
import com.saraasansor.api.model.B2BUnitGroup;
import com.saraasansor.api.service.B2BUnitGroupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/b2bunit-groups")
public class B2BUnitGroupController {

    private final B2BUnitGroupService b2bUnitGroupService;

    public B2BUnitGroupController(B2BUnitGroupService b2bUnitGroupService) {
        this.b2bUnitGroupService = b2bUnitGroupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<B2BUnitGroupDto>>> getAllGroups() {
        List<B2BUnitGroupDto> groups = b2bUnitGroupService.getAllGroups().stream()
                .map(B2BUnitGroupDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<B2BUnitGroupDto>> getGroupById(@PathVariable Long id) {
        B2BUnitGroup group = b2bUnitGroupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success(B2BUnitGroupDto.fromEntity(group)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<B2BUnitGroupDto>> createGroup(
            @Valid @RequestBody CreateB2BUnitGroupRequest request) {
        B2BUnitGroup created = b2bUnitGroupService.createGroup(request);
        return ResponseEntity.ok(ApiResponse.success("B2B unit group successfully created", B2BUnitGroupDto.fromEntity(created)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<B2BUnitGroupDto>> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateB2BUnitGroupRequest request) {
        B2BUnitGroup updated = b2bUnitGroupService.updateGroup(id, request);
        return ResponseEntity.ok(ApiResponse.success("B2B unit group successfully updated", B2BUnitGroupDto.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        b2bUnitGroupService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.success("B2B unit group successfully deleted", null));
    }
}
