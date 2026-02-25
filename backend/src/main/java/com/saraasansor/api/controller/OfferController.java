package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.OfferDto;
import com.saraasansor.api.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/offers")
public class OfferController {
    
    @Autowired
    private OfferService offerService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfferDto>>> getAllOffers() {
        try {
            List<OfferDto> offers = offerService.getAllOffers();
            return ResponseEntity.ok(ApiResponse.success(offers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferDto>> getOfferById(@PathVariable Long id) {
        try {
            OfferDto offer = offerService.getOfferById(id);
            return ResponseEntity.ok(ApiResponse.success(offer));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/elevator/{elevatorId}")
    public ResponseEntity<ApiResponse<List<OfferDto>>> getOffersByElevatorId(@PathVariable Long elevatorId) {
        try {
            List<OfferDto> offers = offerService.getOffersByElevatorId(elevatorId);
            return ResponseEntity.ok(ApiResponse.success(offers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<OfferDto>> createOffer(@Valid @RequestBody OfferDto dto) {
        try {
            OfferDto created = offerService.createOffer(dto);
            return ResponseEntity.ok(ApiResponse.success("Offer successfully created", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferDto>> updateOffer(
            @PathVariable Long id, @Valid @RequestBody OfferDto dto) {
        try {
            OfferDto updated = offerService.updateOffer(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Offer successfully updated", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOffer(@PathVariable Long id) {
        try {
            offerService.deleteOffer(id);
            return ResponseEntity.ok(ApiResponse.success("Offer successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
