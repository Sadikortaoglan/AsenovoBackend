package com.saraasansor.api.revisionstandards.controller;

import com.saraasansor.api.revisionstandards.dto.RevisionStandardImportResponse;
import com.saraasansor.api.revisionstandards.dto.RevisionStandardSearchResponse;
import com.saraasansor.api.revisionstandards.service.RevisionStandardImportService;
import com.saraasansor.api.revisionstandards.service.RevisionStandardSearchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping
public class RevisionStandardsController {

    private final RevisionStandardSearchService revisionStandardSearchService;
    private final RevisionStandardImportService revisionStandardImportService;

    public RevisionStandardsController(RevisionStandardSearchService revisionStandardSearchService,
                                       RevisionStandardImportService revisionStandardImportService) {
        this.revisionStandardSearchService = revisionStandardSearchService;
        this.revisionStandardImportService = revisionStandardImportService;
    }

    @GetMapping("/revision-standards/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RevisionStandardSearchResponse>> search(
            @RequestParam("q") @NotBlank String query,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return ResponseEntity.ok(revisionStandardSearchService.search(query, limit));
    }

    @PostMapping("/admin/revision-standards/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'STAFF_ADMIN')")
    public ResponseEntity<RevisionStandardImportResponse> importStandards() {
        return ResponseEntity.ok(revisionStandardImportService.importFromClasspath());
    }
}
