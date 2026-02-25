package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.BuildingDto;
import com.saraasansor.api.model.Building;
import com.saraasansor.api.service.BuildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/buildings")
public class BuildingController {
    
    @Autowired
    private BuildingService buildingService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<BuildingDto>>> getAllBuildings() {
        try {
            List<BuildingDto> buildings = buildingService.getAllBuildings().stream()
                    .map(BuildingDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(buildings));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingDto>> getBuildingById(@PathVariable Long id) {
        try {
            Building building = buildingService.getBuildingById(id);
            return ResponseEntity.ok(ApiResponse.success(BuildingDto.fromEntity(building)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<BuildingDto>> createBuilding(@RequestBody BuildingDto dto) {
        try {
            Building building = new Building();
            building.setName(dto.getName());
            building.setAddress(dto.getAddress());
            building.setCity(dto.getCity());
            building.setDistrict(dto.getDistrict());
            
            Building created = buildingService.createBuilding(building);
            return ResponseEntity.ok(ApiResponse.success("Building successfully created", BuildingDto.fromEntity(created)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingDto>> updateBuilding(
            @PathVariable Long id, @RequestBody BuildingDto dto) {
        try {
            Building building = new Building();
            building.setName(dto.getName());
            building.setAddress(dto.getAddress());
            building.setCity(dto.getCity());
            building.setDistrict(dto.getDistrict());
            
            Building updated = buildingService.updateBuilding(id, building);
            return ResponseEntity.ok(ApiResponse.success("Building successfully updated", BuildingDto.fromEntity(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBuilding(@PathVariable Long id) {
        try {
            buildingService.deleteBuilding(id);
            return ResponseEntity.ok(ApiResponse.success("Building successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
