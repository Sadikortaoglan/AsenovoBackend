package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelListItemResponse;
import com.saraasansor.api.dto.ElevatorLabelResponse;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.service.ElevatorLabelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping({"/elevator-labels", "/elevator-contracts"})
public class ElevatorLabelController {

    private final ElevatorLabelService elevatorLabelService;

    public ElevatorLabelController(ElevatorLabelService elevatorLabelService) {
        this.elevatorLabelService = elevatorLabelService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(parseFlexibleDate(text));
            }
        });
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ElevatorLabelListItemResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {
        try {
            Pageable pageable = PageRequest.of(page, size, parseSort(sort));
            Page<ElevatorLabelListItemResponse> response = elevatorLabelService.list(pageable, search);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ElevatorLabelResponse>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.success(elevatorLabelService.getById(id)));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ElevatorLabelResponse>> createJson(@Valid @RequestBody ElevatorLabelCreateRequest request) {
        try {
            ElevatorLabelResponse response = elevatorLabelService.create(request);
            return ResponseEntity.ok(ApiResponse.success("Elevator label created", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ElevatorLabelResponse>> createMultipart(
            @ModelAttribute ElevatorLabelCreateRequest request,
            HttpServletRequest httpServletRequest) {
        try {
            normalizeElevatorId(request, httpServletRequest);
            normalizeDateAliases(request, httpServletRequest);
            mergeAdditionalFields(request, httpServletRequest);
            ElevatorLabelResponse response = elevatorLabelService.create(request);
            return ResponseEntity.ok(ApiResponse.success("Elevator label created", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ElevatorLabelResponse>> updateJson(@PathVariable Long id,
                                                                         @Valid @RequestBody ElevatorLabelUpdateRequest request) {
        try {
            ElevatorLabelResponse response = elevatorLabelService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("Elevator label updated", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ElevatorLabelResponse>> updateMultipart(@PathVariable Long id,
                                                                              @ModelAttribute ElevatorLabelUpdateRequest request,
                                                                              HttpServletRequest httpServletRequest) {
        try {
            normalizeElevatorId(request, httpServletRequest);
            normalizeDateAliases(request, httpServletRequest);
            mergeAdditionalFields(request, httpServletRequest);
            ElevatorLabelResponse response = elevatorLabelService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("Elevator label updated", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/{id}")
    public ResponseEntity<ApiResponse<ElevatorLabelResponse>> updatePostFallback(@PathVariable Long id,
                                                                                 @ModelAttribute ElevatorLabelUpdateRequest request,
                                                                                 HttpServletRequest httpServletRequest) {
        try {
            normalizeElevatorId(request, httpServletRequest);
            normalizeDateAliases(request, httpServletRequest);
            mergeAdditionalFields(request, httpServletRequest);
            ElevatorLabelResponse response = elevatorLabelService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("Elevator label updated", response));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private void normalizeElevatorId(ElevatorLabelCreateRequest request, HttpServletRequest httpServletRequest) {
        if (request.getElevatorId() != null) {
            return;
        }
        Long parsed = parseElevatorId(httpServletRequest);
        request.setElevatorId(parsed);
    }

    private void normalizeElevatorId(ElevatorLabelUpdateRequest request, HttpServletRequest httpServletRequest) {
        if (request.getElevatorId() != null) {
            return;
        }
        Long parsed = parseElevatorId(httpServletRequest);
        request.setElevatorId(parsed);
    }

    private void mergeAdditionalFields(ElevatorLabelCreateRequest request, HttpServletRequest httpServletRequest) {
        Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            if (isKnownRequestField(entry.getKey())) {
                continue;
            }
            request.getAdditionalFields().putIfAbsent(entry.getKey(), entry.getValue()[0]);
        }
    }

    private void mergeAdditionalFields(ElevatorLabelUpdateRequest request, HttpServletRequest httpServletRequest) {
        Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            if (isKnownRequestField(entry.getKey())) {
                continue;
            }
            request.getAdditionalFields().putIfAbsent(entry.getKey(), entry.getValue()[0]);
        }
    }

    private void normalizeDateAliases(ElevatorLabelCreateRequest request, HttpServletRequest requestContext) {
        if (request.getLabelStartDate() == null) {
            String value = firstNonBlankParam(requestContext, "labelStartDate", "startAt", "startDate");
            request.setStartAt(value);
        }
        if (request.getLabelEndDate() == null) {
            String value = firstNonBlankParam(requestContext, "labelEndDate", "endAt", "endDate");
            request.setEndAt(value);
        }
        if (request.getLabelIssueDate() == null) {
            String value = firstNonBlankParam(requestContext, "labelIssueDate", "issueDate", "issueAt");
            request.setIssueDate(value);
        }
        if (request.getExpiryDate() == null) {
            String value = firstNonBlankParam(requestContext, "expiryDate", "expiryAt");
            request.setExpiryAt(value);
        }
    }

    private void normalizeDateAliases(ElevatorLabelUpdateRequest request, HttpServletRequest requestContext) {
        if (request.getLabelStartDate() == null) {
            String value = firstNonBlankParam(requestContext, "labelStartDate", "startAt", "startDate");
            request.setStartAt(value);
        }
        if (request.getLabelEndDate() == null) {
            String value = firstNonBlankParam(requestContext, "labelEndDate", "endAt", "endDate");
            request.setEndAt(value);
        }
        if (request.getLabelIssueDate() == null) {
            String value = firstNonBlankParam(requestContext, "labelIssueDate", "issueDate", "issueAt");
            request.setIssueDate(value);
        }
        if (request.getExpiryDate() == null) {
            String value = firstNonBlankParam(requestContext, "expiryDate", "expiryAt");
            request.setExpiryAt(value);
        }
    }

    private Long parseElevatorId(HttpServletRequest request) {
        String[] keys = {"elevatorId", "elevator_id", "selectedElevatorId", "selected_elevator_id", "id", "elevator", "elevator.id"};
        for (String key : keys) {
            String value = request.getParameter(key);
            Long parsed = parseLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstNonBlankParam(HttpServletRequest request, String... keys) {
        for (String key : keys) {
            String value = request.getParameter(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isKnownRequestField(String key) {
        return switch (key) {
            case "elevatorId", "elevator_id", "selectedElevatorId", "selected_elevator_id", "id", "elevator", "elevator.id",
                 "labelName", "labelStartDate", "labelEndDate", "labelIssueDate", "labelDate", "expiryDate",
                 "startAt", "endAt", "startDate", "endDate", "issueDate", "issueAt", "expiryAt",
                 "labelType", "serialNumber", "contractNumber", "description", "status",
                 "file", "payload" -> true;
            default -> false;
        };
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String property = parts.length > 0 && !parts[0].isBlank() ? parts[0] : "updatedAt";
        Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private LocalDate parseFlexibleDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception ignored) {
        }
        int tIndex = normalized.indexOf('T');
        if (tIndex > 0) {
            try {
                return LocalDate.parse(normalized.substring(0, tIndex));
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid date format: " + value);
    }
}
