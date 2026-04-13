package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import com.saraasansor.api.exception.QrCodeNotFoundException;
import com.saraasansor.api.model.LabelType;
import com.saraasansor.api.service.ElevatorQrCodeService;
import com.saraasansor.api.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping({"/qr-codes", "/elevator-labels", "/elevator-qrcodes"})
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
            @RequestParam(required = false) Boolean onlyWithQr,
            HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            Sort sortOrder = parseSort(sort);
            Pageable pageable = PageRequest.of(page, size, sortOrder);
            boolean endpointForElevatorQrcodes = request != null
                    && request.getRequestURI() != null
                    && request.getRequestURI().contains("/elevator-qrcodes");
            boolean effectiveOnlyWithQr = onlyWithQr != null ? onlyWithQr : endpointForElevatorQrcodes;
            Page<QrCodeResponseDTO> qrCodes = qrCodeService.list(pageable, search, companyId, effectiveOnlyWithQr);
            String etag = buildEtag(qrCodes, search, page, size, sort, companyId, effectiveOnlyWithQr);

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
            @RequestParam(required = false) String labelType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String description,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            HttpServletRequest httpRequest) {
        try {
            JsonNode payload = parsePayloadJson(httpRequest);
            ElevatorLabelCreateRequest createRequest = buildCreateRequestFromMultipart(
                    elevatorId, labelType, startDate, endDate, description, httpRequest, payload);
            MultipartFile uploadedFile = file != null ? file : attachment;

            Long companyId = resolveAuthenticatedCompanyId();
            QrCodeResponseDTO created = qrCodeService.create(createRequest, uploadedFile, companyId);
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
                                                                                 @RequestParam(required = false) String labelType,
                                                                                 @RequestParam(required = false) String startDate,
                                                                                 @RequestParam(required = false) String endDate,
                                                                                 @RequestParam(required = false) String description,
                                                                                 @RequestPart(value = "file", required = false) MultipartFile file,
                                                                                 @RequestPart(value = "attachment", required = false) MultipartFile attachment,
                                                                                 HttpServletRequest httpRequest) {
        try {
            JsonNode payload = parsePayloadJson(httpRequest);
            ElevatorLabelUpdateRequest updateRequest = buildUpdateRequestFromMultipart(
                    elevatorId, labelType, startDate, endDate, description, httpRequest, payload);
            MultipartFile uploadedFile = file != null ? file : attachment;

            Long companyId = resolveAuthenticatedCompanyId();
            QrCodeResponseDTO updated = qrCodeService.update(id, updateRequest, uploadedFile, companyId);
            return ResponseEntity.ok(ApiResponse.success("QR code updated", updated));
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
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

    @GetMapping("/{id}/file")
    public ResponseEntity<?> downloadLabelFile(@PathVariable Long id) {
        try {
            Long companyId = resolveAuthenticatedCompanyId();
            ElevatorQrCodeService.LabelFileDownload file = qrCodeService.getLabelFile(id, companyId);
            Resource resource = new FileSystemResource(file.filePath().toFile());
            return ResponseEntity.ok()
                    .contentType(resolveMediaType(file.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizeFileName(file.fileName()) + "\"")
                    .body(resource);
        } catch (QrCodeNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private ElevatorLabelCreateRequest buildCreateRequestFromMultipart(Long directElevatorId,
                                                                       String directLabelType,
                                                                       String directStartDate,
                                                                       String directEndDate,
                                                                       String directDescription,
                                                                       HttpServletRequest request,
                                                                       JsonNode payload) {
        ElevatorLabelCreateRequest createRequest = new ElevatorLabelCreateRequest();
        createRequest.setElevatorId(resolveElevatorId(directElevatorId, request, payload));
        createRequest.setLabelType(parseLabelTypeOrNull(resolveLabelType(directLabelType, request, payload)));
        createRequest.setStartDate(parseLocalDateOrNull(resolveStartDate(directStartDate, request, payload), "startDate"));
        createRequest.setEndDate(parseLocalDateOrNull(resolveEndDate(directEndDate, request, payload), "endDate"));
        createRequest.setDescription(resolveDescription(directDescription, request, payload));
        return createRequest;
    }

    private ElevatorLabelUpdateRequest buildUpdateRequestFromMultipart(Long directElevatorId,
                                                                       String directLabelType,
                                                                       String directStartDate,
                                                                       String directEndDate,
                                                                       String directDescription,
                                                                       HttpServletRequest request,
                                                                       JsonNode payload) {
        ElevatorLabelUpdateRequest updateRequest = new ElevatorLabelUpdateRequest();
        updateRequest.setElevatorId(resolveElevatorId(directElevatorId, request, payload));
        updateRequest.setLabelType(parseLabelTypeOrNull(resolveLabelType(directLabelType, request, payload)));
        updateRequest.setStartDate(parseLocalDateOrNull(resolveStartDate(directStartDate, request, payload), "startDate"));
        updateRequest.setEndDate(parseLocalDateOrNull(resolveEndDate(directEndDate, request, payload), "endDate"));
        updateRequest.setDescription(resolveDescription(directDescription, request, payload));
        return updateRequest;
    }

    private Long resolveElevatorId(Long directElevatorId, HttpServletRequest request, JsonNode payload) {
        if (directElevatorId != null) {
            return directElevatorId;
        }

        Long fromParameters = resolveLongFromParameters(request,
                "elevatorId", "elevator_id", "selectedElevatorId", "selected_elevator_id",
                "elevator.id", "data[elevatorId]", "payload[elevatorId]", "id");
        if (fromParameters != null) {
            return fromParameters;
        }

        Long fromPayload = resolveLongFromPayload(payload,
                "elevatorId", "elevator_id", "selectedElevatorId", "selected_elevator_id",
                "elevator.id", "elevator.elevatorId", "payload.elevatorId", "data.elevatorId", "id");
        if (fromPayload != null) {
            return fromPayload;
        }

        return resolveElevatorIdFromParts(request);
    }

    private String resolveLabelType(String directValue, HttpServletRequest request, JsonNode payload) {
        String fromParams = firstNonBlank(
                directValue,
                request.getParameter("labelType"),
                request.getParameter("label_type"),
                request.getParameter("type"),
                request.getParameter("status"),
                request.getParameter("color"),
                request.getParameter("labelStatus"));
        if (fromParams != null) {
            return fromParams;
        }
        return firstNonBlank(
                textNode(nodeAtPath(payload, "labelType")),
                textNode(nodeAtPath(payload, "label_type")),
                textNode(nodeAtPath(payload, "type")),
                textNode(nodeAtPath(payload, "status")),
                textNode(nodeAtPath(payload, "color")),
                textNode(nodeAtPath(payload, "labelStatus")),
                textNode(nodeAtPath(payload, "label.status")));
    }

    private String resolveStartDate(String directValue, HttpServletRequest request, JsonNode payload) {
        String fromParams = firstNonBlank(
                directValue,
                request.getParameter("startDate"),
                request.getParameter("start_date"),
                request.getParameter("labelDate"),
                request.getParameter("label_date"),
                request.getParameter("date"),
                request.getParameter("transferDate"));
        if (fromParams != null) {
            return fromParams;
        }
        return firstNonBlank(
                textNode(nodeAtPath(payload, "startDate")),
                textNode(nodeAtPath(payload, "start_date")),
                textNode(nodeAtPath(payload, "labelDate")),
                textNode(nodeAtPath(payload, "label_date")),
                textNode(nodeAtPath(payload, "date")),
                textNode(nodeAtPath(payload, "transferDate")));
    }

    private String resolveEndDate(String directValue, HttpServletRequest request, JsonNode payload) {
        String fromParams = firstNonBlank(
                directValue,
                request.getParameter("endDate"),
                request.getParameter("end_date"),
                request.getParameter("expiryDate"),
                request.getParameter("expiry_date"),
                request.getParameter("expirationDate"));
        if (fromParams != null) {
            return fromParams;
        }
        return firstNonBlank(
                textNode(nodeAtPath(payload, "endDate")),
                textNode(nodeAtPath(payload, "end_date")),
                textNode(nodeAtPath(payload, "expiryDate")),
                textNode(nodeAtPath(payload, "expiry_date")),
                textNode(nodeAtPath(payload, "expirationDate")));
    }

    private String resolveDescription(String directValue, HttpServletRequest request, JsonNode payload) {
        String fromParams = firstNonBlank(
                directValue,
                request.getParameter("description"),
                request.getParameter("desc"),
                request.getParameter("aciklama"),
                request.getParameter("notes"));
        if (fromParams != null) {
            return fromParams;
        }
        return firstNonBlank(
                textNode(nodeAtPath(payload, "description")),
                textNode(nodeAtPath(payload, "desc")),
                textNode(nodeAtPath(payload, "aciklama")),
                textNode(nodeAtPath(payload, "notes")));
    }

    private LabelType parseLabelTypeOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('İ', 'I')
                .replace('Ş', 'S')
                .replace('Ğ', 'G')
                .replace('Ü', 'U')
                .replace('Ö', 'O')
                .replace('Ç', 'C');

        normalized = switch (normalized) {
            case "YESIL" -> "GREEN";
            case "SARI" -> "YELLOW";
            case "KIRMIZI" -> "RED";
            case "TURUNCU" -> "ORANGE";
            case "MAVI" -> "BLUE";
            default -> normalized;
        };

        try {
            return LabelType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid labelType: " + value);
        }
    }

    private LocalDate parseLocalDateOrNull(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmed = value.trim();
        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (DateTimeParseException ignored) {
        }

        if (trimmed.length() >= 10) {
            String firstTen = trimmed.substring(0, 10);
            try {
                return LocalDate.parse(firstTen, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new RuntimeException("Invalid " + fieldName + " format: " + value);
    }

    private Long resolveLongFromParameters(HttpServletRequest request, String... keys) {
        for (String key : keys) {
            Long parsed = parseLongOrNull(request.getParameter(key));
            if (parsed != null) {
                return parsed;
            }
        }

        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null) {
            return null;
        }

        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            Long parsed = parseLongOrNull(entry.getValue()[0]);
            if (parsed != null && (entry.getKey().toLowerCase(Locale.ROOT).contains("elevator")
                    || "id".equalsIgnoreCase(entry.getKey()))) {
                return parsed;
            }
        }
        return null;
    }

    private Long resolveLongFromPayload(JsonNode payload, String... paths) {
        if (payload == null || payload.isMissingNode() || payload.isNull()) {
            return null;
        }
        for (String path : paths) {
            Long parsed = parseLongNode(nodeAtPath(payload, path));
            if (parsed != null) {
                return parsed;
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
                String body = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                if (body.isEmpty()) {
                    continue;
                }
                Long parsed = parseLongOrNull(body);
                String loweredName = name.toLowerCase(Locale.ROOT);
                if (parsed != null && (loweredName.contains("elevator") || "id".equals(loweredName))) {
                    return parsed;
                }
                JsonNode parsedJson = parseJson(body);
                Long fromJson = resolveLongFromPayload(parsedJson,
                        "elevatorId", "elevator_id", "selectedElevatorId",
                        "selected_elevator_id", "elevator.id", "payload.elevatorId", "data.elevatorId");
                if (fromJson != null) {
                    return fromJson;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private JsonNode parsePayloadJson(HttpServletRequest request) {
        JsonNode direct = firstParsedJson(
                request.getParameter("payload"),
                request.getParameter("data"),
                request.getParameter("label"),
                request.getParameter("request"),
                request.getParameter("qrCode"),
                request.getParameter("entity")
        );
        if (direct != null) {
            return direct;
        }

        try {
            Collection<Part> parts = request.getParts();
            if (parts != null) {
                for (Part part : parts) {
                    String partName = part.getName() == null ? "" : part.getName().toLowerCase(Locale.ROOT);
                    if (!partName.contains("payload")
                            && !partName.contains("data")
                            && !partName.contains("label")
                            && !partName.contains("request")
                            && !partName.contains("qrcode")
                            && !partName.contains("entity")) {
                        continue;
                    }
                    String body = new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    JsonNode parsed = parseJson(body);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap != null) {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                if (entry.getValue() == null || entry.getValue().length == 0) {
                    continue;
                }
                JsonNode parsed = parseJson(entry.getValue()[0]);
                if (parsed != null) {
                    return parsed;
                }
            }
        }

        return null;
    }

    private JsonNode firstParsedJson(String... candidates) {
        for (String candidate : candidates) {
            JsonNode parsed = parseJson(candidate);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private JsonNode parseJson(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonNode nodeAtPath(JsonNode root, String path) {
        if (root == null || root.isNull() || root.isMissingNode() || !StringUtils.hasText(path)) {
            return null;
        }
        String[] tokens = path.split("\\.");
        JsonNode current = root;
        for (String token : tokens) {
            if (current == null || current.isNull() || current.isMissingNode()) {
                return null;
            }
            current = current.path(token);
        }
        return current;
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

    private String textNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.textValue();
            return StringUtils.hasText(value) ? value.trim() : null;
        }
        if (node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        return null;
    }

    private Long parseLongOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private MediaType resolveMediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "label-file";
        }
        return fileName.replace("\"", "");
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
        return 1L;
    }

    private String buildEtag(Page<QrCodeResponseDTO> qrCodes, String search, int page, int size, String sort, Long companyId, boolean onlyWithQr) {
        StringBuilder builder = new StringBuilder();
        builder.append("company=").append(companyId)
                .append("|search=").append(search == null ? "" : search)
                .append("|page=").append(page)
                .append("|size=").append(size)
                .append("|sort=").append(sort)
                .append("|onlyWithQr=").append(onlyWithQr)
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
