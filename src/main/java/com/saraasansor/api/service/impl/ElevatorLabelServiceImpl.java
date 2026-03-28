package com.saraasansor.api.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelListItemResponse;
import com.saraasansor.api.dto.ElevatorLabelResponse;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorLabel;
import com.saraasansor.api.repository.ElevatorLabelRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.service.ElevatorLabelService;
import com.saraasansor.api.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class ElevatorLabelServiceImpl implements ElevatorLabelService {

    private final ElevatorLabelRepository elevatorLabelRepository;
    private final ElevatorRepository elevatorRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ElevatorLabelServiceImpl(ElevatorLabelRepository elevatorLabelRepository,
                                    ElevatorRepository elevatorRepository,
                                    FileStorageService fileStorageService,
                                    ObjectMapper objectMapper) {
        this.elevatorLabelRepository = elevatorLabelRepository;
        this.elevatorRepository = elevatorRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ElevatorLabelListItemResponse> list(Pageable pageable, String search) {
        String normalizedSearch = search == null ? "" : search.trim();
        return elevatorLabelRepository.search(normalizedSearch, pageable).map(this::toListItemResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ElevatorLabelResponse getById(Long id) {
        ElevatorLabel label = elevatorLabelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator label not found: " + id));
        return toResponse(label);
    }

    @Override
    public ElevatorLabelResponse create(ElevatorLabelCreateRequest request) {
        Elevator elevator = resolveElevator(request.getElevatorId());
        validateDates(request.getLabelStartDate(), request.getLabelEndDate());

        ElevatorLabel label = new ElevatorLabel();
        label.setElevator(elevator);
        applyCreateRequest(label, request);
        setAuditForCreate(label);

        ElevatorLabel saved = elevatorLabelRepository.save(label);
        MultipartFile uploadedFile = resolveUploadedFile(request.getFile(), request.getPayload());
        if (uploadedFile != null) {
            applyAttachment(saved, uploadedFile);
            setAuditForUpdate(saved);
            saved = elevatorLabelRepository.save(saved);
        }
        return toResponse(saved);
    }

    @Override
    public ElevatorLabelResponse update(Long id, ElevatorLabelUpdateRequest request) {
        ElevatorLabel label = elevatorLabelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Elevator label not found: " + id));
        Elevator elevator = resolveElevator(request.getElevatorId());
        validateDates(request.getLabelStartDate(), request.getLabelEndDate());

        label.setElevator(elevator);
        applyUpdateRequest(label, request);
        MultipartFile uploadedFile = resolveUploadedFile(request.getFile(), request.getPayload());
        if (uploadedFile != null) {
            applyAttachment(label, uploadedFile);
        }
        setAuditForUpdate(label);

        ElevatorLabel saved = elevatorLabelRepository.save(label);
        return toResponse(saved);
    }

    private Elevator resolveElevator(Long elevatorId) {
        if (elevatorId == null) {
            throw new RuntimeException("elevatorId is required");
        }
        return elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new RuntimeException("Elevator not found: " + elevatorId));
    }

    private void validateDates(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new RuntimeException("labelEndDate must be on or after labelStartDate");
        }
    }

    private void applyCreateRequest(ElevatorLabel label, ElevatorLabelCreateRequest request) {
        label.setLabelName(trimToNull(request.getLabelName()));
        label.setLabelStartDate(request.getLabelStartDate());
        label.setLabelEndDate(request.getLabelEndDate());
        label.setLabelIssueDate(request.getLabelIssueDate());
        label.setLabelDate(request.getLabelDate());
        label.setExpiryDate(request.getExpiryDate());
        label.setLabelType(trimToNull(request.getLabelType()));
        label.setSerialNumber(trimToNull(request.getSerialNumber()));
        label.setContractNumber(trimToNull(request.getContractNumber()));
        label.setDescription(trimToNull(request.getDescription()));
        label.setStatus(defaultStatus(request.getStatus()));
        label.setMetadataJson(writeMetadata(request.getAdditionalFields()));
    }

    private void applyUpdateRequest(ElevatorLabel label, ElevatorLabelUpdateRequest request) {
        label.setLabelName(trimToNull(request.getLabelName()));
        label.setLabelStartDate(request.getLabelStartDate());
        label.setLabelEndDate(request.getLabelEndDate());
        label.setLabelIssueDate(request.getLabelIssueDate());
        label.setLabelDate(request.getLabelDate());
        label.setExpiryDate(request.getExpiryDate());
        label.setLabelType(trimToNull(request.getLabelType()));
        label.setSerialNumber(trimToNull(request.getSerialNumber()));
        label.setContractNumber(trimToNull(request.getContractNumber()));
        label.setDescription(trimToNull(request.getDescription()));
        label.setStatus(defaultStatus(request.getStatus()));
        label.setMetadataJson(writeMetadata(request.getAdditionalFields()));
    }

    private ElevatorLabelListItemResponse toListItemResponse(ElevatorLabel label) {
        ElevatorLabelListItemResponse response = new ElevatorLabelListItemResponse();
        response.setId(label.getId());
        response.setElevatorId(label.getElevator().getId());
        response.setElevatorName(resolveElevatorName(label.getElevator()));
        response.setFacilityId(label.getElevator().getFacility() != null ? label.getElevator().getFacility().getId() : null);
        response.setFacilityName(resolveFacilityName(label.getElevator()));
        response.setLabelName(nullSafe(label.getLabelName()));
        response.setLabelStartDate(label.getLabelStartDate());
        response.setLabelEndDate(label.getLabelEndDate());
        response.setLabelIssueDate(label.getLabelIssueDate());
        response.setLabelDate(label.getLabelDate());
        response.setExpiryDate(label.getExpiryDate());
        response.setLabelType(nullSafe(label.getLabelType()));
        response.setSerialNumber(nullSafe(label.getSerialNumber()));
        response.setContractNumber(nullSafe(label.getContractNumber()));
        response.setStatus(nullSafe(label.getStatus()));
        response.setAttachmentName(nullSafe(label.getAttachmentName()));
        response.setAttachmentUrl(nullSafe(label.getAttachmentUrl()));
        response.setAttachmentExists(label.getAttachmentUrl() != null && !label.getAttachmentUrl().isBlank());
        response.setCreatedAt(label.getCreatedAt());
        response.setUpdatedAt(label.getUpdatedAt());
        return response;
    }

    private ElevatorLabelResponse toResponse(ElevatorLabel label) {
        ElevatorLabelResponse response = new ElevatorLabelResponse();
        response.setId(label.getId());
        response.setElevatorId(label.getElevator().getId());
        response.setElevatorName(resolveElevatorName(label.getElevator()));
        response.setFacilityId(label.getElevator().getFacility() != null ? label.getElevator().getFacility().getId() : null);
        response.setFacilityName(resolveFacilityName(label.getElevator()));
        response.setLabelName(nullSafe(label.getLabelName()));
        response.setLabelStartDate(label.getLabelStartDate());
        response.setLabelEndDate(label.getLabelEndDate());
        response.setLabelIssueDate(label.getLabelIssueDate());
        response.setLabelDate(label.getLabelDate());
        response.setExpiryDate(label.getExpiryDate());
        response.setLabelType(nullSafe(label.getLabelType()));
        response.setSerialNumber(nullSafe(label.getSerialNumber()));
        response.setContractNumber(nullSafe(label.getContractNumber()));
        response.setDescription(nullSafe(label.getDescription()));
        response.setStatus(nullSafe(label.getStatus()));
        response.setQrCodeId(label.getQrCode() != null ? label.getQrCode().getId() : null);
        response.setAttachmentName(nullSafe(label.getAttachmentName()));
        response.setAttachmentContentType(nullSafe(label.getAttachmentContentType()));
        response.setAttachmentSize(label.getAttachmentSize());
        response.setAttachmentUrl(nullSafe(label.getAttachmentUrl()));
        response.setAttachmentExists(label.getAttachmentUrl() != null && !label.getAttachmentUrl().isBlank());
        response.setCreatedAt(label.getCreatedAt());
        response.setUpdatedAt(label.getUpdatedAt());
        response.setAdditionalFields(readMetadata(label.getMetadataJson()));
        return response;
    }

    private String resolveElevatorName(Elevator elevator) {
        if (elevator.getElevatorNumber() != null && !elevator.getElevatorNumber().isBlank()) {
            return elevator.getElevatorNumber();
        }
        if (elevator.getIdentityNumber() != null && !elevator.getIdentityNumber().isBlank()) {
            return elevator.getIdentityNumber();
        }
        return "Elevator #" + elevator.getId();
    }

    private String resolveFacilityName(Elevator elevator) {
        if (elevator.getFacility() != null && elevator.getFacility().getName() != null) {
            return elevator.getFacility().getName();
        }
        return nullSafe(elevator.getBuildingName());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String defaultStatus(String status) {
        String normalized = trimToNull(status);
        return normalized == null ? "ACTIVE" : normalized;
    }

    private MultipartFile resolveUploadedFile(MultipartFile file, MultipartFile payload) {
        if (file != null && !file.isEmpty()) {
            return file;
        }
        if (payload != null && !payload.isEmpty()) {
            return payload;
        }
        return null;
    }

    private void applyAttachment(ElevatorLabel label, MultipartFile uploadedFile) {
        if (uploadedFile.isEmpty()) {
            return;
        }
        if (uploadedFile.getSize() <= 0) {
            throw new RuntimeException("Uploaded file is empty");
        }
        try {
            String storageKey = fileStorageService.saveFile(uploadedFile, "ELEVATOR_LABEL", label.getId());
            String url = fileStorageService.getFileUrl(storageKey);
            label.setAttachmentStorageKey(storageKey);
            label.setAttachmentUrl(url);
            label.setAttachmentName(trimToNull(uploadedFile.getOriginalFilename()));
            label.setAttachmentContentType(trimToNull(uploadedFile.getContentType()));
            label.setAttachmentSize(uploadedFile.getSize());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store label file", ex);
        }
    }

    private String writeMetadata(Map<String, Object> additionalFields) {
        Map<String, Object> safe = additionalFields == null ? Map.of() : additionalFields;
        if (safe.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize label metadata", ex);
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private void setAuditForCreate(ElevatorLabel label) {
        String actor = resolveActorUsername();
        label.setCreatedBy(actor);
        label.setUpdatedBy(actor);
    }

    private void setAuditForUpdate(ElevatorLabel label) {
        label.setUpdatedBy(resolveActorUsername());
    }

    private String resolveActorUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return "system";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return String.valueOf(principal);
    }
}
