package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.RevisionOfferDto;
import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.RevisionOffer;
import com.saraasansor.api.service.RevisionOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/revision-offers")
public class RevisionOfferController {
    
    @Autowired
    private RevisionOfferService revisionOfferService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<RevisionOfferDto>>> getAllRevisionOffers() {
        try {
            List<RevisionOfferDto> offers = revisionOfferService.getAllRevisionOffers().stream()
                    .map(RevisionOfferDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(offers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RevisionOfferDto>> getRevisionOfferById(@PathVariable Long id) {
        try {
            RevisionOffer offer = revisionOfferService.getRevisionOfferById(id);
            return ResponseEntity.ok(ApiResponse.success(RevisionOfferDto.fromEntity(offer)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/elevator/{elevatorId}")
    public ResponseEntity<ApiResponse<List<RevisionOfferDto>>> getByElevatorId(@PathVariable Long elevatorId) {
        try {
            List<RevisionOfferDto> offers = revisionOfferService.getByElevatorId(elevatorId).stream()
                    .map(RevisionOfferDto::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(offers));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<RevisionOfferDto>> createRevisionOffer(@RequestBody RevisionOfferDto dto) {
        try {
            RevisionOffer offer = new RevisionOffer();
            
            Elevator elevator = new Elevator();
            elevator.setId(dto.getElevatorId());
            offer.setElevator(elevator);
            
            if (dto.getBuildingId() != null) {
                Building building = new Building();
                building.setId(dto.getBuildingId());
                offer.setBuilding(building);
            }
            
            if (dto.getCurrentAccountId() != null) {
                CurrentAccount account = new CurrentAccount();
                account.setId(dto.getCurrentAccountId());
                offer.setCurrentAccount(account);
            }
            
            offer.setPartsTotal(dto.getPartsTotal() != null ? dto.getPartsTotal() : BigDecimal.ZERO);
            offer.setLaborTotal(dto.getLaborTotal() != null ? dto.getLaborTotal() : BigDecimal.ZERO);
            
            if (dto.getStatus() != null) {
                try {
                    offer.setStatus(RevisionOffer.Status.valueOf(dto.getStatus().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    offer.setStatus(RevisionOffer.Status.DRAFT);
                }
            }
            
            RevisionOffer created = revisionOfferService.createRevisionOffer(offer);
            return ResponseEntity.ok(ApiResponse.success("Revision offer successfully created", RevisionOfferDto.fromEntity(created)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RevisionOfferDto>> updateRevisionOffer(
            @PathVariable Long id, @RequestBody RevisionOfferDto dto) {
        try {
            RevisionOffer offer = revisionOfferService.getRevisionOfferById(id);
            
            if (dto.getPartsTotal() != null) {
                offer.setPartsTotal(dto.getPartsTotal());
            }
            if (dto.getLaborTotal() != null) {
                offer.setLaborTotal(dto.getLaborTotal());
            }
            if (dto.getStatus() != null) {
                try {
                    offer.setStatus(RevisionOffer.Status.valueOf(dto.getStatus().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid status: " + dto.getStatus());
                }
            }
            
            RevisionOffer updated = revisionOfferService.updateRevisionOffer(id, offer);
            return ResponseEntity.ok(ApiResponse.success("Revision offer successfully updated", RevisionOfferDto.fromEntity(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/convert-to-sale")
    public ResponseEntity<ApiResponse<RevisionOfferDto>> convertToSale(@PathVariable Long id) {
        try {
            RevisionOffer converted = revisionOfferService.convertToSale(id);
            return ResponseEntity.ok(ApiResponse.success("Revision offer converted to sale", RevisionOfferDto.fromEntity(converted)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRevisionOffer(@PathVariable Long id) {
        try {
            revisionOfferService.deleteRevisionOffer(id);
            return ResponseEntity.ok(ApiResponse.success("Revision offer successfully deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
