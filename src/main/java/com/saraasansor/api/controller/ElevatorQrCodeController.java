package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import com.saraasansor.api.exception.QrCodeNotFoundException;
import com.saraasansor.api.service.ElevatorQrCodeService;
import com.saraasansor.api.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@RestController
@RequestMapping("/qr-codes")
public class ElevatorQrCodeController {

    private final ElevatorQrCodeService qrCodeService;

    public ElevatorQrCodeController(ElevatorQrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<QrCodeResponseDTO>>> getQrCodes(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            Sort sortOrder = parseSort(sort);
            Pageable pageable = PageRequest.of(page, size, sortOrder);
            Page<QrCodeResponseDTO> qrCodes = qrCodeService.list(pageable, search, companyId);
            String etag = buildEtag(qrCodes, search, page, size, sort, companyId);

            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                        .eTag(etag)
                        .build();
            }

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                    .eTag(etag)
                    .body(ApiResponse.success(qrCodes));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/{elevatorId}")
    public ResponseEntity<ApiResponse<QrCodeResponseDTO>> createQrCode(@PathVariable Long elevatorId) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            QrCodeResponseDTO created = qrCodeService.create(elevatorId, companyId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("QR code created", created));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQrCode(@PathVariable Long id) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            qrCodeService.delete(id, companyId);
            return ResponseEntity.noContent().build();
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping(value = "/{id}/print", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> printQrCode(@PathVariable Long id) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            byte[] png = qrCodeService.generateQrImage(id, companyId);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(png);
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String property = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "createdAt";
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private Long resolveAuthenticatedCompanyId() {
        Long companyId = TenantContext.getTenantId();
        if (companyId == null) {
            throw new RuntimeException("Company context is missing");
        }
        return companyId;
    }

    private String buildEtag(Page<QrCodeResponseDTO> qrCodes, String search, int page, int size, String sort, Long companyId) {
        StringBuilder builder = new StringBuilder();
        builder.append("company=").append(companyId)
                .append("|search=").append(search == null ? "" : search)
                .append("|page=").append(page)
                .append("|size=").append(size)
                .append("|sort=").append(sort)
                .append("|total=").append(qrCodes.getTotalElements());

        qrCodes.getContent().forEach(item -> builder.append("|")
                .append(item.getElevatorId()).append(":")
                .append(item.getId()).append(":")
                .append(item.isHasQr()).append(":")
                .append(item.getCreatedAt()));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "\"" + hex + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to build ETag", e);
        }
    }
}
