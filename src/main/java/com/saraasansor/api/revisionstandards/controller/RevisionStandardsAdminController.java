package com.saraasansor.api.revisionstandards.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.revisionstandards.dto.CreateRevisionStandardArticleRequest;
import com.saraasansor.api.revisionstandards.dto.CreateRevisionStandardSetRequest;
import com.saraasansor.api.revisionstandards.dto.RevisionStandardArticleResponse;
import com.saraasansor.api.revisionstandards.dto.RevisionStandardSetResponse;
import com.saraasansor.api.revisionstandards.dto.UpdateRevisionStandardArticleRequest;
import com.saraasansor.api.revisionstandards.dto.UpdateRevisionStandardSetRequest;
import com.saraasansor.api.revisionstandards.service.RevisionStandardAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin/revision-standards")
@PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_ADMIN', 'STAFF_ADMIN')")
public class RevisionStandardsAdminController {

    private final RevisionStandardAdminService revisionStandardAdminService;

    public RevisionStandardsAdminController(RevisionStandardAdminService revisionStandardAdminService) {
        this.revisionStandardAdminService = revisionStandardAdminService;
    }

    @GetMapping("/standards")
    public ResponseEntity<ApiResponse<Page<RevisionStandardSetResponse>>> getStandards(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(revisionStandardAdminService.getStandardSets(query, page, size)));
    }

    @GetMapping("/standards/{standardId}")
    public ResponseEntity<ApiResponse<RevisionStandardSetResponse>> getStandard(@PathVariable Long standardId) {
        return ResponseEntity.ok(ApiResponse.success(revisionStandardAdminService.getStandardSet(standardId)));
    }

    @PostMapping("/standards")
    public ResponseEntity<ApiResponse<RevisionStandardSetResponse>> createStandard(
            @Valid @RequestBody CreateRevisionStandardSetRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Standart basariyla olusturuldu",
                revisionStandardAdminService.createStandardSet(request)));
    }

    @PutMapping("/standards/{standardId}")
    public ResponseEntity<ApiResponse<RevisionStandardSetResponse>> updateStandard(
            @PathVariable Long standardId,
            @Valid @RequestBody UpdateRevisionStandardSetRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Standart basariyla guncellendi",
                revisionStandardAdminService.updateStandardSet(standardId, request)));
    }

    @DeleteMapping("/standards/{standardId}")
    public ResponseEntity<ApiResponse<Void>> deleteStandard(@PathVariable Long standardId) {
        revisionStandardAdminService.deleteStandardSet(standardId);
        return ResponseEntity.ok(ApiResponse.success("Standart basariyla silindi", null));
    }

    @GetMapping("/standards/{standardId}/articles")
    public ResponseEntity<ApiResponse<Page<RevisionStandardArticleResponse>>> getArticles(
            @PathVariable Long standardId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tagColor,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(
                revisionStandardAdminService.getArticles(standardId, query, tagColor, minPrice, maxPrice, page, size)
        ));
    }

    @PostMapping("/standards/{standardId}/articles")
    public ResponseEntity<ApiResponse<RevisionStandardArticleResponse>> createArticle(
            @PathVariable Long standardId,
            @Valid @RequestBody CreateRevisionStandardArticleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Madde basariyla olusturuldu",
                revisionStandardAdminService.createArticle(standardId, request)));
    }

    @PutMapping("/articles/{articleId}")
    public ResponseEntity<ApiResponse<RevisionStandardArticleResponse>> updateArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody UpdateRevisionStandardArticleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Madde basariyla guncellendi",
                revisionStandardAdminService.updateArticle(articleId, request)));
    }

    @DeleteMapping("/articles/{articleId}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long articleId) {
        revisionStandardAdminService.deleteArticle(articleId);
        return ResponseEntity.ok(ApiResponse.success("Madde basariyla silindi", null));
    }
}
