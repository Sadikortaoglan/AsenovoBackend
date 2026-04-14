package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorContractCreateRequest;
import com.saraasansor.api.dto.ElevatorContractListItemResponse;
import com.saraasansor.api.dto.ElevatorContractResponse;
import com.saraasansor.api.dto.ElevatorContractUpdateRequest;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.exception.ValidationException;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorContract;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorContractRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class ElevatorContractService {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy")
    );

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    );

    private final ElevatorContractRepository elevatorContractRepository;
    private final ElevatorRepository elevatorRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public ElevatorContractService(ElevatorContractRepository elevatorContractRepository,
                                   ElevatorRepository elevatorRepository,
                                   UserRepository userRepository,
                                   FileStorageService fileStorageService) {
        this.elevatorContractRepository = elevatorContractRepository;
        this.elevatorRepository = elevatorRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public Page<ElevatorContractListItemResponse> list(String search, Long elevatorId, Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<ElevatorContract> page;

        if (isCariUser(currentUser)) {
            Long b2bUnitId = resolveCurrentUserB2bUnitId(currentUser);
            page = elevatorContractRepository.searchByB2bUnit(b2bUnitId, search, elevatorId, pageable);
        } else {
            page = elevatorContractRepository.search(search, elevatorId, pageable);
        }

        return page.map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public ElevatorContractResponse getById(Long id) {
        User currentUser = getCurrentUser();
        ElevatorContract contract = elevatorContractRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Elevator contract not found: " + id));
        ensureContractAccess(contract, currentUser);
        return toResponse(contract);
    }

    @Transactional(readOnly = true)
    public ContractFileDownload getContractFile(Long id) {
        User currentUser = getCurrentUser();
        ElevatorContract contract = elevatorContractRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Elevator contract not found: " + id));
        ensureContractAccess(contract, currentUser);

        if (!StringUtils.hasText(contract.getAttachmentStorageKey())) {
            throw new NotFoundException("Elevator contract file not found: " + id);
        }

        Path filePath;
        try {
            filePath = fileStorageService.resolveStoredPath(contract.getAttachmentStorageKey());
        } catch (RuntimeException ex) {
            throw new NotFoundException("Elevator contract file not found: " + id);
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new NotFoundException("Elevator contract file not found: " + id);
        }

        String fileName = StringUtils.hasText(contract.getAttachmentOriginalFileName())
                ? contract.getAttachmentOriginalFileName()
                : (filePath.getFileName() != null ? filePath.getFileName().toString() : ("contract-" + id));

        String contentType = resolveContentType(filePath, contract.getAttachmentContentType());
        return new ContractFileDownload(filePath, fileName, contentType);
    }

    public ElevatorContractResponse create(ElevatorContractCreateRequest request, MultipartFile file) {
        if (request == null || request.getElevatorId() == null) {
            throw new ValidationException("elevatorId is required");
        }

        User currentUser = getCurrentUser();
        ElevatorContract contract = new ElevatorContract();
        contract.setElevator(resolveAccessibleElevator(request.getElevatorId(), currentUser));
        contract.setContractDate(parseContractDate(request.getContractDate()));
        contract.setContractHtml(request.getContractHtml());
        contract.setStatus(normalizeStatus(request.getStatus(), "ACTIVE"));
        contract.setCreatedBy(currentUser);
        contract.setUpdatedBy(currentUser);
        contract.setUpdatedAt(LocalDateTime.now());

        ElevatorContract saved = elevatorContractRepository.save(contract);
        if (file != null && !file.isEmpty()) {
            saved = storeAttachment(saved, file);
        }

        return toResponse(saved);
    }

    public ElevatorContractResponse update(Long id, ElevatorContractUpdateRequest request, MultipartFile file) {
        User currentUser = getCurrentUser();
        ElevatorContract contract = elevatorContractRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Elevator contract not found: " + id));
        ensureContractAccess(contract, currentUser);

        if (request != null) {
            if (request.getElevatorId() != null) {
                contract.setElevator(resolveAccessibleElevator(request.getElevatorId(), currentUser));
            }
            if (request.getContractDate() != null) {
                contract.setContractDate(parseContractDate(request.getContractDate()));
            }
            if (request.getContractHtml() != null) {
                contract.setContractHtml(request.getContractHtml());
            }
            if (request.getStatus() != null) {
                contract.setStatus(normalizeStatus(request.getStatus(), contract.getStatus()));
            }
        }

        contract.setUpdatedBy(currentUser);
        contract.setUpdatedAt(LocalDateTime.now());

        ElevatorContract saved = elevatorContractRepository.save(contract);
        if (file != null && !file.isEmpty()) {
            saved = storeAttachment(saved, file);
        }

        return toResponse(saved);
    }

    public void delete(Long id) {
        User currentUser = getCurrentUser();
        ElevatorContract contract = elevatorContractRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("Elevator contract not found: " + id));
        ensureContractAccess(contract, currentUser);
        elevatorContractRepository.delete(contract);
    }

    private ElevatorContract storeAttachment(ElevatorContract contract, MultipartFile file) {
        try {
            String storageKey = fileStorageService.saveFile(file, "ELEVATOR_CONTRACT", contract.getId());
            contract.setAttachmentStorageKey(storageKey);
            contract.setAttachmentOriginalFileName(file.getOriginalFilename());
            contract.setAttachmentContentType(file.getContentType());
            contract.setAttachmentSize(file.getSize());
            contract.setUpdatedAt(LocalDateTime.now());
            return elevatorContractRepository.save(contract);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save contract file: " + ex.getMessage(), ex);
        }
    }

    private Elevator resolveAccessibleElevator(Long elevatorId, User currentUser) {
        Elevator elevator = elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new ValidationException("Elevator not found: " + elevatorId));

        if (isCariUser(currentUser)) {
            Long currentUserB2bUnitId = resolveCurrentUserB2bUnitId(currentUser);
            Long elevatorB2bUnitId = extractElevatorB2bUnitId(elevator);
            if (!Objects.equals(currentUserB2bUnitId, elevatorB2bUnitId)) {
                throw new AccessDeniedException("You do not have access to the selected elevator");
            }
        }
        return elevator;
    }

    private void ensureContractAccess(ElevatorContract contract, User currentUser) {
        if (!isCariUser(currentUser)) {
            return;
        }
        Long currentUserB2bUnitId = resolveCurrentUserB2bUnitId(currentUser);
        Long contractB2bUnitId = extractElevatorB2bUnitId(contract.getElevator());
        if (!Objects.equals(currentUserB2bUnitId, contractB2bUnitId)) {
            throw new AccessDeniedException("You do not have access to this contract");
        }
    }

    private Long extractElevatorB2bUnitId(Elevator elevator) {
        if (elevator == null || elevator.getFacility() == null || elevator.getFacility().getB2bUnit() == null) {
            return null;
        }
        return elevator.getFacility().getB2bUnit().getId();
    }

    private Long resolveCurrentUserB2bUnitId(User currentUser) {
        if (currentUser.getB2bUnit() == null || currentUser.getB2bUnit().getId() == null) {
            throw new AccessDeniedException("Current user has no b2bUnit scope");
        }
        return currentUser.getB2bUnit().getId();
    }

    private boolean isCariUser(User user) {
        User.Role canonical = user.getCanonicalRole();
        return canonical != null && canonical.isCariUser();
    }

    private LocalDate parseContractDate(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return null;
        }

        String value = rawDate.trim();

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter).toLocalDate();
            } catch (DateTimeParseException ignored) {
            }
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        if (value.length() >= 10) {
            String firstTen = value.substring(0, 10);
            try {
                return LocalDate.parse(firstTen, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new ValidationException("Invalid contractDate format: " + rawDate);
    }

    private String normalizeStatus(String status, String defaultValue) {
        if (!StringUtils.hasText(status)) {
            return defaultValue;
        }
        return status.trim().toUpperCase();
    }

    private ElevatorContractListItemResponse toListItem(ElevatorContract contract) {
        ElevatorContractListItemResponse response = new ElevatorContractListItemResponse();
        response.setId(contract.getId());
        response.setElevatorId(contract.getElevator() != null ? contract.getElevator().getId() : null);
        response.setElevatorName(resolveElevatorName(contract.getElevator()));
        response.setFacilityId(contract.getElevator() != null && contract.getElevator().getFacility() != null
                ? contract.getElevator().getFacility().getId()
                : null);
        response.setFacilityName(contract.getElevator() != null && contract.getElevator().getFacility() != null
                ? contract.getElevator().getFacility().getName()
                : null);
        response.setContractDate(contract.getContractDate());
        response.setStatus(contract.getStatus());
        boolean hasFile = StringUtils.hasText(contract.getAttachmentStorageKey());
        response.setAttachmentExists(hasFile);
        response.setHasFile(hasFile);
        response.setFileName(contract.getAttachmentOriginalFileName());
        response.setAttachmentUrl(hasFile ? "/api/elevator-contracts/" + contract.getId() + "/file" : null);
        response.setCreatedAt(contract.getCreatedAt());
        response.setUpdatedAt(contract.getUpdatedAt());
        return response;
    }

    private ElevatorContractResponse toResponse(ElevatorContract contract) {
        ElevatorContractResponse response = new ElevatorContractResponse();
        response.setId(contract.getId());
        response.setElevatorId(contract.getElevator() != null ? contract.getElevator().getId() : null);
        response.setElevatorName(resolveElevatorName(contract.getElevator()));
        response.setFacilityId(contract.getElevator() != null && contract.getElevator().getFacility() != null
                ? contract.getElevator().getFacility().getId()
                : null);
        response.setFacilityName(contract.getElevator() != null && contract.getElevator().getFacility() != null
                ? contract.getElevator().getFacility().getName()
                : null);
        response.setContractDate(contract.getContractDate());
        response.setContractHtml(contract.getContractHtml());
        response.setStatus(contract.getStatus());
        response.setAttachmentOriginalFileName(contract.getAttachmentOriginalFileName());
        response.setAttachmentContentType(contract.getAttachmentContentType());
        response.setAttachmentSize(contract.getAttachmentSize());
        boolean hasFile = StringUtils.hasText(contract.getAttachmentStorageKey());
        response.setAttachmentExists(hasFile);
        response.setHasFile(hasFile);
        response.setFileName(contract.getAttachmentOriginalFileName());
        response.setAttachmentUrl(hasFile ? "/api/elevator-contracts/" + contract.getId() + "/file" : null);
        response.setCreatedAt(contract.getCreatedAt());
        response.setUpdatedAt(contract.getUpdatedAt());
        return response;
    }

    private String resolveContentType(Path filePath, String storedContentType) {
        if (StringUtils.hasText(storedContentType)) {
            return storedContentType;
        }
        try {
            String detected = Files.probeContentType(filePath);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (Exception ignored) {
        }
        return "application/octet-stream";
    }

    private String resolveElevatorName(Elevator elevator) {
        if (elevator == null) {
            return null;
        }
        if (StringUtils.hasText(elevator.getElevatorNumber())) {
            return elevator.getElevatorNumber();
        }
        if (StringUtils.hasText(elevator.getIdentityNumber())) {
            return elevator.getIdentityNumber();
        }
        return "Elevator #" + elevator.getId();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new AccessDeniedException("User not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public record ContractFileDownload(Path filePath, String fileName, String contentType) {
    }
}
