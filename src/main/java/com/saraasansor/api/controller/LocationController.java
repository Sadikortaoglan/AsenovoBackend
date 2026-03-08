package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.LocationOptionDto;
import com.saraasansor.api.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<LocationOptionDto>>> getCities(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getCities(query)));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<LocationOptionDto>>> getDistricts(
            @RequestParam Long cityId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getDistricts(cityId, query)));
    }

    @GetMapping("/neighborhoods")
    public ResponseEntity<ApiResponse<List<LocationOptionDto>>> getNeighborhoods(
            @RequestParam Long districtId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getNeighborhoods(districtId, query)));
    }

    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<List<LocationOptionDto>>> getRegions(
            @RequestParam Long neighborhoodId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.success(locationService.getRegions(neighborhoodId, query)));
    }
}
