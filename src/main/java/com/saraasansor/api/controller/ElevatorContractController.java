package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorContractCreateRequest;
import com.saraasansor.api.dto.ElevatorContractListItemResponse;
import com.saraasansor.api.dto.ElevatorContractResponse;
import com.saraasansor.api.dto.ElevatorContractUpdateRequest;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.service.ElevatorContractService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/elevator-contracts")
public class ElevatorContractController {

    private final ElevatorContractService elevatorContractService;
    private final ObjectMapper objectMapper;

    public ElevatorContractController(ElevatorContractService elevatorContractService,
                                      ObjectMapper objectMapper) {
        this.elevatorContractService = elevatorContractService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ElevatorContractListItemResponse>>> getContracts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long elevatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<ElevatorContractListItemResponse> response = elevatorContractService.list(search, elevatorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ElevatorContractResponse>> getContractById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(elevatorContractService.getById(id)));
    }

    @GetMapping({"/{id}/file", "/{id}/attachment"})
    public ResponseEntity<?> downloadContractFile(@PathVariable Long id) {
        try {
            ElevatorContractService.ContractFileDownload file = elevatorContractService.getContractFile(id);
            Resource resource = new FileSystemResource(file.filePath().toFile());
            return ResponseEntity.ok()
                    .contentType(resolveMediaType(file.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizeFileName(file.fileName()) + "\"")
                    .body(resource);
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ElevatorContractResponse>> createContractJson(
            @Valid @RequestBody ElevatorContractCreateRequest request) {
        ElevatorContractResponse created = elevatorContractService.create(request, null);
        return ResponseEntity.ok(ApiResponse.success("Elevator contract created", created));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ElevatorContractResponse>> createContractMultipart(
            @RequestParam(required = false) Long elevatorId,
            @RequestParam(required = false) String contractDate,
            @RequestParam(required = false) String contractHtml,
            @RequestParam(required = false) String status,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            HttpServletRequest httpRequest) {
        MultipartFields resolved = resolveMultipartFields(elevatorId, contractDate, contractHtml, status, httpRequest);

        ElevatorContractCreateRequest request = new ElevatorContractCreateRequest();
        request.setElevatorId(resolved.elevatorId());
        request.setContractDate(resolved.contractDate());
        request.setContractHtml(resolved.contractHtml());
        request.setStatus(resolved.status());

        MultipartFile uploadedFile = file != null ? file : attachment;
        ElevatorContractResponse created = elevatorContractService.create(request, uploadedFile);
        return ResponseEntity.ok(ApiResponse.success("Elevator contract created", created));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ElevatorContractResponse>> updateContractJson(
            @PathVariable Long id,
            @Valid @RequestBody ElevatorContractUpdateRequest request) {
        ElevatorContractResponse updated = elevatorContractService.update(id, request, null);
        return ResponseEntity.ok(ApiResponse.success("Elevator contract updated", updated));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ElevatorContractResponse>> updateContractMultipart(
            @PathVariable Long id,
            @RequestParam(required = false) Long elevatorId,
            @RequestParam(required = false) String contractDate,
            @RequestParam(required = false) String contractHtml,
            @RequestParam(required = false) String status,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment,
            HttpServletRequest httpRequest) {
        MultipartFields resolved = resolveMultipartFields(elevatorId, contractDate, contractHtml, status, httpRequest);

        ElevatorContractUpdateRequest request = new ElevatorContractUpdateRequest();
        request.setElevatorId(resolved.elevatorId());
        request.setContractDate(resolved.contractDate());
        request.setContractHtml(resolved.contractHtml());
        request.setStatus(resolved.status());

        MultipartFile uploadedFile = file != null ? file : attachment;
        ElevatorContractResponse updated = elevatorContractService.update(id, request, uploadedFile);
        return ResponseEntity.ok(ApiResponse.success("Elevator contract updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContract(@PathVariable Long id) {
        elevatorContractService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Elevator contract deleted", null));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())) {
            direction = Sort.Direction.ASC;
        }
        return Sort.by(direction, field);
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
            return "contract-file";
        }
        return fileName.replace("\"", "");
    }

    private MultipartFields resolveMultipartFields(Long elevatorId,
                                                   String contractDate,
                                                   String contractHtml,
                                                   String status,
                                                   HttpServletRequest request) {
        Long resolvedElevatorId = elevatorId;
        String resolvedContractDate = contractDate;
        String resolvedContractHtml = contractHtml;
        String resolvedStatus = status;

        if (resolvedElevatorId == null) {
            resolvedElevatorId = parseLongOrNull(firstNonBlank(
                    request.getParameter("elevatorId"),
                    request.getParameter("elevator_id"),
                    request.getParameter("selectedElevatorId"),
                    request.getParameter("selected_elevator_id")
            ));
        }

        if (resolvedContractDate == null) {
            resolvedContractDate = firstNonBlank(
                    request.getParameter("contractDate"),
                    request.getParameter("contract_date"),
                    request.getParameter("date"),
                    request.getParameter("startAt"),
                    request.getParameter("startDate")
            );
        }

        if (resolvedContractHtml == null) {
            resolvedContractHtml = firstNonBlank(
                    request.getParameter("contractHtml"),
                    request.getParameter("contract_html"),
                    request.getParameter("html"),
                    request.getParameter("content")
            );
        }

        if (resolvedStatus == null) {
            resolvedStatus = firstNonBlank(
                    request.getParameter("status"),
                    request.getParameter("contractStatus")
            );
        }

        JsonNode payload = parsePayloadJson(request);
        if (payload != null) {
            if (resolvedElevatorId == null) {
                resolvedElevatorId = parseLongNode(payload.path("elevatorId"));
                if (resolvedElevatorId == null) {
                    resolvedElevatorId = parseLongNode(payload.path("elevator_id"));
                }
                if (resolvedElevatorId == null) {
                    JsonNode elevatorNode = payload.path("elevator");
                    if (elevatorNode != null && !elevatorNode.isMissingNode()) {
                        resolvedElevatorId = parseLongNode(elevatorNode.path("id"));
                    }
                }
            }

            if (resolvedContractDate == null) {
                resolvedContractDate = firstNonBlank(
                        textNode(payload.path("contractDate")),
                        textNode(payload.path("contract_date")),
                        textNode(payload.path("date")),
                        textNode(payload.path("startAt")),
                        textNode(payload.path("startDate"))
                );
            }

            if (resolvedContractHtml == null) {
                resolvedContractHtml = firstNonBlank(
                        textNode(payload.path("contractHtml")),
                        textNode(payload.path("contract_html")),
                        textNode(payload.path("html")),
                        textNode(payload.path("content"))
                );
            }

            if (resolvedStatus == null) {
                resolvedStatus = firstNonBlank(
                        textNode(payload.path("status")),
                        textNode(payload.path("contractStatus"))
                );
            }
        }

        return new MultipartFields(resolvedElevatorId, resolvedContractDate, resolvedContractHtml, resolvedStatus);
    }

    private JsonNode parsePayloadJson(HttpServletRequest request) {
        String[] jsonCandidates = {
                request.getParameter("payload"),
                request.getParameter("data"),
                request.getParameter("contract"),
                request.getParameter("request")
        };

        for (String candidate : jsonCandidates) {
            JsonNode parsed = parseJson(candidate);
            if (parsed != null) {
                return parsed;
            }
        }

        try {
            Collection<Part> parts = request.getParts();
            if (parts == null) {
                return null;
            }
            for (Part part : parts) {
                String partName = part.getName() == null ? "" : part.getName().toLowerCase();
                if (!partName.contains("payload")
                        && !partName.contains("data")
                        && !partName.contains("contract")
                        && !partName.contains("request")) {
                    continue;
                }
                String body = new String(part.getInputStream().readAllBytes()).trim();
                JsonNode parsed = parseJson(body);
                if (parsed != null) {
                    return parsed;
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

    private JsonNode parseJson(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long parseLongNode(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
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
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.textValue();
            return value == null || value.isBlank() ? null : value;
        }
        return node.toString();
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record MultipartFields(Long elevatorId, String contractDate, String contractHtml, String status) {
    }
}
