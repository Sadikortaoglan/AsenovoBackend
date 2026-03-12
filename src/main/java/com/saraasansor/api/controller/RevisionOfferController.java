package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.RevisionOfferDto;
import com.saraasansor.api.dto.RevisionOfferItemDto;
import com.saraasansor.api.dto.RevisionStandardReferenceDto;
import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.RevisionOffer;
import com.saraasansor.api.service.RevisionOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    public ResponseEntity<ApiResponse<List<RevisionOfferDto>>> getAllRevisionOffers(
            @RequestParam(required = false) String status) {
        try {
            List<RevisionOfferDto> offers = revisionOfferService.getAllRevisionOffers(status).stream()
                    .map(this::toDto)
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
            return ResponseEntity.ok(ApiResponse.success(toDto(offer)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/elevator/{elevatorId}")
    public ResponseEntity<ApiResponse<List<RevisionOfferDto>>> getByElevatorId(@PathVariable Long elevatorId) {
        try {
            List<RevisionOfferDto> offers = revisionOfferService.getByElevatorId(elevatorId).stream()
                    .map(this::toDto)
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

            if (dto.getRevisionStandardId() != null) {
                offer.setRevisionStandardId(dto.getRevisionStandardId());
            }

            offer.setLaborTotal(resolveLabor(dto));
            offer.setLaborDescription(dto.getLaborDescription());
            revisionOfferService.replaceItems(offer, revisionOfferService.buildItems(resolveItems(dto)));
            
            if (dto.getStatus() != null) {
                offer.setStatus(parseStatus(dto.getStatus(), RevisionOffer.Status.DRAFT));
            }
            
            RevisionOffer created = revisionOfferService.createRevisionOffer(offer);
            return ResponseEntity.ok(ApiResponse.success("Revision offer successfully created", toDto(created)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RevisionOfferDto>> updateRevisionOffer(
            @PathVariable Long id, @RequestBody RevisionOfferDto dto) {
        try {
            RevisionOffer updated = doUpdateRevisionOffer(id, dto);
            return ResponseEntity.ok(ApiResponse.success(resolveUpdateSuccessMessage(dto, updated), toDto(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<ApiResponse<RevisionOfferDto>> updateRevisionOffer(@RequestBody RevisionOfferDto dto) {
        try {
            if (dto.getId() == null) {
                throw new RuntimeException("Revision offer id is required");
            }
            RevisionOffer updated = doUpdateRevisionOffer(dto.getId(), dto);
            return ResponseEntity.ok(ApiResponse.success(resolveUpdateSuccessMessage(dto, updated), toDto(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/convert-to-sale")
    public ResponseEntity<ApiResponse<RevisionOfferDto>> convertToSale(@PathVariable Long id) {
        try {
            RevisionOffer converted = revisionOfferService.convertToSale(id);
            return ResponseEntity.ok(ApiResponse.success("Revision offer converted to sale", toDto(converted)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> downloadRevisionOfferPdf(@PathVariable Long id) {
        try {
            byte[] pdfBytes = revisionOfferService.generateRevisionOfferPdf(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"revision-offer-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(pdfBytes);
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

    private RevisionOfferDto toDto(RevisionOffer offer) {
        RevisionOfferDto dto = RevisionOfferDto.fromEntity(offer);
        String revisionStandardCode = revisionOfferService.getRevisionStandardCode(offer.getRevisionStandardId());
        dto.setRevisionStandardCode(revisionStandardCode);
        if (dto.getRevisionStandardId() != null || revisionStandardCode != null) {
            dto.setRevisionStandard(new RevisionStandardReferenceDto(dto.getRevisionStandardId(), revisionStandardCode));
        }
        if (dto.getOfferItems().isEmpty() && !dto.getItems().isEmpty()) {
            dto.setOfferItems(dto.getItems());
        }
        return dto;
    }

    private BigDecimal resolveLabor(RevisionOfferDto dto) {
        if (dto.getLabor() != null) {
            return dto.getLabor();
        }
        return dto.getLaborTotal() != null ? dto.getLaborTotal() : BigDecimal.ZERO;
    }

    private List<RevisionOfferItemDto> resolveItems(RevisionOfferDto dto) {
        if (dto.getParts() != null && !dto.getParts().isEmpty()) {
            return dto.getParts();
        }
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            return dto.getItems();
        }
        if (dto.getOfferItems() != null && !dto.getOfferItems().isEmpty()) {
            return dto.getOfferItems();
        }
        return null;
    }

    private RevisionOffer doUpdateRevisionOffer(Long id, RevisionOfferDto dto) {
        RevisionOffer current = revisionOfferService.getRevisionOfferById(id);
        RevisionOffer offer = new RevisionOffer();
        offer.setId(id);
        offer.setItems(null);

        if (dto.getPartsTotal() != null) {
            offer.setPartsTotal(dto.getPartsTotal());
        }
        if (dto.getLabor() != null || dto.getLaborTotal() != null) {
            offer.setLaborTotal(resolveLabor(dto));
        } else {
            offer.setLaborTotal(current.getLaborTotal());
        }
        if (dto.getLaborDescription() != null) {
            offer.setLaborDescription(dto.getLaborDescription());
        }
        List<RevisionOfferItemDto> requestItems = resolveItems(dto);
        if (requestItems != null) {
            offer.setItems(new java.util.ArrayList<>());
            revisionOfferService.replaceItems(offer, revisionOfferService.buildItems(requestItems));
        }
        if (dto.getStatus() != null) {
            offer.setStatus(parseStatus(dto.getStatus(), null));
        }

        if (dto.getRevisionStandardId() != null) {
            offer.setRevisionStandardId(dto.getRevisionStandardId());
        }

        return revisionOfferService.updateRevisionOffer(id, offer);
    }

    private RevisionOffer.Status parseStatus(String rawStatus, RevisionOffer.Status defaultStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return defaultStatus;
        }
        String normalized = rawStatus.trim().toUpperCase();
        return switch (normalized) {
            case "DRAFT" -> RevisionOffer.Status.DRAFT;
            case "SENT" -> RevisionOffer.Status.SENT;
            case "ACCEPTED", "APPROVED" -> RevisionOffer.Status.APPROVED;
            case "REJECTED" -> RevisionOffer.Status.REJECTED;
            case "CONVERTED", "CONVERTED_TO_SALE" -> RevisionOffer.Status.CONVERTED_TO_SALE;
            default -> throw new RuntimeException("Invalid status: " + rawStatus);
        };
    }

    private String resolveUpdateSuccessMessage(RevisionOfferDto request, RevisionOffer updated) {
        if (request.getStatus() == null) {
            return "Revision offer successfully updated";
        }
        String normalizedRequest = request.getStatus().trim().toUpperCase();
        if ("SENT".equals(normalizedRequest)) {
            return "Revision offer sent successfully";
        }
        if ("REJECTED".equals(normalizedRequest)) {
            return "Revision offer rejected successfully";
        }
        if ("ACCEPTED".equals(normalizedRequest) || "APPROVED".equals(normalizedRequest)) {
            if (updated.getStatus() == RevisionOffer.Status.CONVERTED_TO_SALE) {
                return "Revision offer approved and converted to sale successfully";
            }
            return "Revision offer approved successfully";
        }
        return "Revision offer successfully updated";
    }
}
