package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.OfferDto;
import com.saraasansor.api.dto.ProposalLineItemRequest;
import com.saraasansor.api.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/proposals")
public class ProposalController {
    @Autowired
    private ProposalService proposalService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OfferDto>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(proposalService.list(pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OfferDto>> create(@Valid @RequestBody OfferDto dto) {
        return ResponseEntity.status(201).body(ApiResponse.success("Proposal created", proposalService.create(dto)));
    }

    @PostMapping("/{proposalId}/items")
    public ResponseEntity<ApiResponse<OfferDto>> addLineItem(
            @PathVariable Long proposalId,
            @Valid @RequestBody ProposalLineItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Proposal line item added", proposalService.addLineItem(proposalId, request)));
    }

    @DeleteMapping("/{proposalId}/items/{itemId}")
    public ResponseEntity<ApiResponse<OfferDto>> removeLineItem(
            @PathVariable Long proposalId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(ApiResponse.success("Proposal line item removed", proposalService.removeLineItem(proposalId, itemId)));
    }
}
