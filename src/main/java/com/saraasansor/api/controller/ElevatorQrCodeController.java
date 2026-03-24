package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import com.saraasansor.api.exception.QrCodeNotFoundException;
import com.saraasansor.api.service.ElevatorQrCodeService;
import com.saraasansor.api.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
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
import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping({"/qr-codes", "/elevator-labels"})
public class ElevatorQrCodeController {

    private final ElevatorQrCodeService qrCodeService;
    private final ObjectMapper objectMapper;

    public ElevatorQrCodeController(ElevatorQrCodeService qrCodeService, ObjectMapper objectMapper) {
        this.qrCodeService = qrCodeService;
        this.objectMapper = objectMapper;
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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QrCodeResponseDTO>> getQrCodeById(@PathVariable Long id) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            QrCodeResponseDTO response = qrCodeService.getById(id, companyId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<QrCodeResponseDTO>> createQrCode(@Valid @RequestBody ElevatorLabelCreateRequest request) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            QrCodeResponseDTO created = qrCodeService.create(request, companyId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("QR code created", created));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QrCodeResponseDTO>> createQrCodeMultipart(
            @RequestParam(required = false) Long elevatorId,
            HttpServletRequest httpRequest) {
        try {
            Long resolvedElevatorId = resolveElevatorId(elevatorId, httpRequest);
            if (resolvedElevatorId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("elevatorId is required"));
            }
            Long companyId = resolveAuthenticatedCompanyId();
            ElevatorLabelCreateRequest createRequest = new ElevatorLabelCreateRequest();
            createRequest.setElevatorId(resolvedElevatorId);
            QrCodeResponseDTO created = qrCodeService.create(createRequest, companyId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("QR code created", created));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
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

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<QrCodeResponseDTO>> updateQrCode(@PathVariable Long id,
                                                                       @Valid @RequestBody ElevatorLabelUpdateRequest request) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            QrCodeResponseDTO updated = qrCodeService.update(id, request, companyId);
            return ResponseEntity.ok(ApiResponse.success("QR code updated", updated));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QrCodeResponseDTO>> updateQrCodeMultipart(@PathVariable Long id,
                                                                                 @RequestParam(required = false) Long elevatorId,
                                                                                 HttpServletRequest httpRequest) {
        try {
            Long resolvedElevatorId = resolveElevatorId(elevatorId, httpRequest);
            if (resolvedElevatorId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("elevatorId is required"));
            }
            Long companyId = resolveAuthenticatedCompanyId();
            ElevatorLabelUpdateRequest updateRequest = new ElevatorLabelUpdateRequest();
            updateRequest.setElevatorId(resolvedElevatorId);
            QrCodeResponseDTO updated = qrCodeService.update(id, updateRequest, companyId);
            return ResponseEntity.ok(ApiResponse.success("QR code updated", updated));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private Long resolveElevatorId(Long directElevatorId, HttpServletRequest request) {
        if (directElevatorId != null) {
            return directElevatorId;
        }
        Long fromParams = resolveElevatorIdFromParameters(request);
        if (fromParams != null) {
            return fromParams;
        }
        return resolveElevatorIdFromParts(request);
    }

    private Long resolveElevatorIdFromParameters(HttpServletRequest request) {
        String[] candidateKeys = {
                "elevatorId",
                "elevator_id",
                "selectedElevatorId",
                "selected_elevator_id",
                "id",
                "elevator",
                "elevator.id",
                "data[elevatorId]",
                "payload[elevatorId]"
        };
        for (String key : candidateKeys) {
            Long parsed = parseLongOrNull(request.getParameter(key));
            if (parsed != null) {
                return parsed;
            }
        }
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase();
            String value = entry.getValue()[0];
            Long parsed = parseLongOrNull(value);
            if (parsed != null && (key.contains("elevator") || "id".equals(key))) {
                return parsed;
            }
            Long fromJson = parseElevatorIdFromJson(value);
            if (fromJson != null) {
                return fromJson;
            }
        }
        return null;
    }

    private Long resolveElevatorIdFromParts(HttpServletRequest request) {
        try {
            Collection<Part> parts = request.getParts();
            if (parts == null || parts.isEmpty()) {
                return null;
            }
            for (Part part : parts) {
                String name = part.getName();
                if (name == null) {
                    continue;
                }
                String body = new String(part.getInputStream().readAllBytes()).trim();
                if (body.isEmpty()) {
                    continue;
                }
                Long parsed = parseLongOrNull(body);
                String loweredName = name.toLowerCase();
                if (parsed != null && (loweredName.contains("elevator") || "id".equals(loweredName))) {
                    return parsed;
                }
                Long fromJson = parseElevatorIdFromJson(body);
                if (fromJson != null) {
                    return fromJson;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Long parseElevatorIdFromJson(String value) {
        if (value == null || value.isBlank() || !value.trim().startsWith("{")) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(value);
            JsonNode elevatorIdNode = root.path("elevatorId");
            Long parsed = parseLongNode(elevatorIdNode);
            if (parsed != null) {
                return parsed;
            }
            JsonNode altNode = root.path("elevator_id");
            parsed = parseLongNode(altNode);
            if (parsed != null) {
                return parsed;
            }
            JsonNode selectedNode = root.path("selectedElevatorId");
            parsed = parseLongNode(selectedNode);
            if (parsed != null) {
                return parsed;
            }
            JsonNode elevatorNode = root.path("elevator");
            if (!elevatorNode.isMissingNode() && !elevatorNode.isNull()) {
                parsed = parseLongNode(elevatorNode.path("id"));
                if (parsed != null) {
                    return parsed;
                }
                parsed = parseLongNode(elevatorNode.path("elevatorId"));
                if (parsed != null) {
                    return parsed;
                }
            }
            JsonNode payloadNode = root.path("payload");
            if (!payloadNode.isMissingNode() && !payloadNode.isNull()) {
                parsed = parseLongNode(payloadNode.path("elevatorId"));
                if (parsed != null) {
                    return parsed;
                }
            }
            JsonNode dataNode = root.path("data");
            if (!dataNode.isMissingNode() && !dataNode.isNull()) {
                parsed = parseLongNode(dataNode.path("elevatorId"));
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Long parseLongNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.longValue();
        }
        if (node.isTextual()) {
            return parseLongOrNull(node.textValue());
        }
        return null;
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
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
        if (companyId != null) {
            return companyId;
        }
        // Legacy/single-tenant compatibility:
        // When tenant context is not resolved (e.g. localhost/default host),
        // keep module operational with deterministic local scope key.
        return 1L;
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
