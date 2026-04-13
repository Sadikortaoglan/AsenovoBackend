package com.saraasansor.api.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import com.saraasansor.api.exception.QrCodeNotFoundException;
import com.saraasansor.api.exception.ValidationException;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorQrCode;
import com.saraasansor.api.model.LabelType;
import com.saraasansor.api.repository.ElevatorQrCodeRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.service.ElevatorQrService;
import com.saraasansor.api.service.ElevatorQrCodeService;
import com.saraasansor.api.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ElevatorQrCodeServiceImpl implements ElevatorQrCodeService {

    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10L * 1024L * 1024L;

    private final ElevatorQrCodeRepository qrCodeRepository;
    private final ElevatorRepository elevatorRepository;
    private final ElevatorQrService elevatorQrService;
    private final FileStorageService fileStorageService;

    public ElevatorQrCodeServiceImpl(ElevatorQrCodeRepository qrCodeRepository,
                                     ElevatorRepository elevatorRepository,
                                     ElevatorQrService elevatorQrService,
                                     FileStorageService fileStorageService) {
        this.qrCodeRepository = qrCodeRepository;
        this.elevatorRepository = elevatorRepository;
        this.elevatorQrService = elevatorQrService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QrCodeResponseDTO> list(Pageable pageable, String search, Long companyId) {
        return list(pageable, search, companyId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QrCodeResponseDTO> list(Pageable pageable, String search, Long companyId, boolean onlyWithQr) {
        return qrCodeRepository.findAllBySearchAndCompanyId(search, companyId, onlyWithQr, pageable).map(this::toDto);
    }

    @Override
    public QrCodeResponseDTO create(Long elevatorId, Long companyId) {
        ElevatorLabelCreateRequest request = new ElevatorLabelCreateRequest();
        request.setElevatorId(elevatorId);
        return create(request, companyId);
    }

    @Override
    public QrCodeResponseDTO create(ElevatorLabelCreateRequest request, Long companyId) {
        return create(request, null, companyId);
    }

    @Override
    public QrCodeResponseDTO create(ElevatorLabelCreateRequest request, MultipartFile file, Long companyId) {
        ElevatorLabelCreateRequest normalized = normalizeCreateRequest(request);
        Elevator elevator = resolveElevator(normalized.getElevatorId());
        LabelFields labelFields = resolveLabelFieldsForCreate(elevator, normalized);

        ElevatorQrCode qrCode = new ElevatorQrCode();
        qrCode.setUuid(UUID.randomUUID());
        qrCode.setElevator(elevator);
        qrCode.setQrValue(generateUniqueQrValue());
        qrCode.setCompanyId(companyId);
        applyLabelFields(qrCode, labelFields, normalized.getDescription());

        ElevatorQrCode saved = qrCodeRepository.save(qrCode);
        if (file != null && !file.isEmpty()) {
            saved = storeAttachment(saved, file);
        }
        return toDto(saved);
    }

    @Override
    public QrCodeResponseDTO update(Long id, ElevatorLabelUpdateRequest request, Long companyId) {
        return update(id, request, null, companyId);
    }

    @Override
    public QrCodeResponseDTO update(Long id, ElevatorLabelUpdateRequest request, MultipartFile file, Long companyId) {
        ElevatorQrCode qrCode = findByIdAndCompanyId(id, companyId);
        ElevatorLabelUpdateRequest normalized = normalizeUpdateRequest(request);
        Elevator elevator = resolveElevator(normalized.getElevatorId());
        LabelFields labelFields = resolveLabelFieldsForUpdate(qrCode, elevator, normalized);
        qrCode.setElevator(elevator);
        applyLabelFields(qrCode, labelFields, normalized.getDescription());

        ElevatorQrCode saved = qrCodeRepository.save(qrCode);
        if (file != null && !file.isEmpty()) {
            saved = storeAttachment(saved, file);
        }
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QrCodeResponseDTO getById(Long id, Long companyId) {
        return toDto(findByIdAndCompanyId(id, companyId));
    }

    @Override
    public void delete(Long id, Long companyId) {
        ElevatorQrCode qrCode = findByIdAndCompanyId(id, companyId);
        qrCodeRepository.delete(qrCode);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrImage(Long id, Long companyId) {
        ElevatorQrCode qrCode = findByIdAndCompanyId(id, companyId);
        String qrPayload = buildPublicQrPayload(qrCode.getElevator().getId());
        return generateQrImageFromValue(qrPayload, 300);
    }

    @Override
    @Transactional(readOnly = true)
    public LabelFileDownload getLabelFile(Long id, Long companyId) {
        ElevatorQrCode qrCode = findByIdAndCompanyId(id, companyId);
        if (!StringUtils.hasText(qrCode.getAttachmentStorageKey())) {
            throw new QrCodeNotFoundException("Label file not found: " + id);
        }

        Path filePath = fileStorageService.resolveStoredPath(qrCode.getAttachmentStorageKey());
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new QrCodeNotFoundException("Label file not found: " + id);
        }

        String fileName = StringUtils.hasText(qrCode.getAttachmentOriginalFileName())
                ? qrCode.getAttachmentOriginalFileName()
                : (filePath.getFileName() != null ? filePath.getFileName().toString() : ("elevator-label-" + id));

        String contentType = resolveContentType(filePath, qrCode.getAttachmentContentType());
        return new LabelFileDownload(filePath, fileName, contentType);
    }

    private byte[] generateQrImageFromValue(String qrValue, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(qrValue, BarcodeFormat.QR_CODE, size, size, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR image", e);
        }
    }

    private String generateUniqueQrValue() {
        String value;
        do {
            value = "ELEVATOR_QR_" + UUID.randomUUID();
        } while (qrCodeRepository.existsByQrValue(value));
        return value;
    }

    private ElevatorQrCode findByIdAndCompanyId(Long id, Long companyId) {
        return qrCodeRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + id));
    }

    private Elevator resolveElevator(Long elevatorId) {
        if (elevatorId == null) {
            throw new ValidationException("elevatorId is required");
        }
        return elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new QrCodeNotFoundException("Elevator not found: " + elevatorId));
    }

    private QrCodeResponseDTO toDto(ElevatorQrCode entity) {
        QrCodeResponseDTO dto = new QrCodeResponseDTO();
        Elevator elevator = entity.getElevator();
        String elevatorName = resolveElevatorName(elevator.getElevatorNumber(), elevator.getIdentityNumber(), elevator.getId());
        String buildingName = nullSafe(elevator.getBuildingName());
        String facilityName = elevator.getFacility() != null
                ? nullSafe(elevator.getFacility().getName())
                : buildingName;
        String customerName = nullSafe(elevator.getManagerName());
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid());
        dto.setElevatorId(elevator.getId());
        dto.setElevatorName(elevatorName);
        dto.setBuildingName(buildingName);
        dto.setFacilityId(elevator.getFacility() != null ? elevator.getFacility().getId() : null);
        dto.setFacilityName(facilityName);
        dto.setB2bUnitId(elevator.getFacility() != null && elevator.getFacility().getB2bUnit() != null
                ? elevator.getFacility().getB2bUnit().getId()
                : null);
        dto.setB2bUnitName(elevator.getFacility() != null && elevator.getFacility().getB2bUnit() != null
                ? nullSafe(elevator.getFacility().getB2bUnit().getName())
                : "");
        dto.setCustomerName(customerName);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setLabelType(entity.getLabelType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setDescription(entity.getDescription());
        dto.setAttachmentOriginalFileName(entity.getAttachmentOriginalFileName());
        dto.setAttachmentContentType(entity.getAttachmentContentType());
        dto.setAttachmentSize(entity.getAttachmentSize());
        boolean attachmentExists = StringUtils.hasText(entity.getAttachmentStorageKey());
        dto.setAttachmentExists(attachmentExists);
        dto.setAttachmentUrl(attachmentExists
                ? fileStorageService.getFileUrl(entity.getAttachmentStorageKey())
                : null);
        dto.setHasQr(true);
        dto.setQrImageUrl("/api/elevators/" + elevator.getId() + "/qr");
        dto.setQrPrintUrl("/api/elevator-qrcodes/" + entity.getId() + "/print");
        String qrPayload = buildPublicQrPayload(elevator.getId());
        dto.setQrPngBase64(Base64.getEncoder().encodeToString(generateQrImageFromValue(qrPayload, 120)));
        return dto;
    }

    private QrCodeResponseDTO toDto(ElevatorQrCodeRepository.ElevatorQrListProjection projection) {
        QrCodeResponseDTO dto = new QrCodeResponseDTO();
        String elevatorName = resolveElevatorName(projection.getElevatorName(), null, projection.getElevatorId());
        String buildingName = nullSafe(projection.getBuildingName());
        String facilityName = nullSafe(projection.getFacilityName());
        if (facilityName.isEmpty()) {
            facilityName = buildingName;
        }
        dto.setId(projection.getQrId());
        dto.setUuid(projection.getUuid());
        dto.setElevatorId(projection.getElevatorId());
        dto.setElevatorName(elevatorName);
        dto.setBuildingName(buildingName);
        dto.setFacilityId(projection.getFacilityId());
        dto.setFacilityName(facilityName);
        dto.setB2bUnitId(projection.getB2bUnitId());
        dto.setB2bUnitName(nullSafe(projection.getB2bUnitName()));
        dto.setCustomerName(nullSafe(projection.getCustomerName()));
        dto.setCreatedAt(projection.getCreatedAt());
        dto.setUpdatedAt(projection.getUpdatedAt());
        dto.setLabelType(projection.getLabelType());
        dto.setStartDate(projection.getStartDate());
        dto.setEndDate(projection.getEndDate());
        dto.setDescription(projection.getDescription());
        dto.setAttachmentOriginalFileName(projection.getAttachmentOriginalFileName());
        dto.setAttachmentContentType(projection.getAttachmentContentType());
        dto.setAttachmentSize(projection.getAttachmentSize());
        boolean attachmentExists = StringUtils.hasText(projection.getAttachmentStorageKey());
        dto.setAttachmentExists(attachmentExists);
        dto.setAttachmentUrl(attachmentExists
                ? fileStorageService.getFileUrl(projection.getAttachmentStorageKey())
                : null);
        boolean hasQr = projection.getQrId() != null && projection.getQrValue() != null;
        dto.setHasQr(hasQr);
        if (hasQr) {
            String qrPayload = buildPublicQrPayload(projection.getElevatorId());
            dto.setQrImageUrl("/api/elevators/" + projection.getElevatorId() + "/qr");
            dto.setQrPrintUrl("/api/elevator-qrcodes/" + projection.getQrId() + "/print");
            dto.setQrPngBase64(Base64.getEncoder().encodeToString(generateQrImageFromValue(qrPayload, 120)));
        } else {
            dto.setQrImageUrl(null);
            dto.setQrPrintUrl(null);
            dto.setQrPngBase64(null);
        }
        return dto;
    }

    private String buildPublicQrPayload(Long elevatorId) {
        return elevatorQrService.generateQrUrl(elevatorId);
    }

    private String resolveElevatorName(String elevatorNumber, String identityNumber, Long elevatorId) {
        if (elevatorNumber != null && !elevatorNumber.isBlank()) {
            return elevatorNumber.trim();
        }
        if (identityNumber != null && !identityNumber.isBlank()) {
            return identityNumber.trim();
        }
        return elevatorId != null ? "Elevator #" + elevatorId : "";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private ElevatorLabelCreateRequest normalizeCreateRequest(ElevatorLabelCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.getElevatorId() == null) {
            throw new ValidationException("elevatorId is required");
        }
        request.setDescription(trimToNull(request.getDescription()));
        return request;
    }

    private ElevatorLabelUpdateRequest normalizeUpdateRequest(ElevatorLabelUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.getElevatorId() == null) {
            throw new ValidationException("elevatorId is required");
        }
        request.setDescription(trimToNull(request.getDescription()));
        return request;
    }

    private LabelFields resolveLabelFieldsForCreate(Elevator elevator, ElevatorLabelCreateRequest request) {
        LabelFields fields = new LabelFields();
        fields.labelType = request.getLabelType() != null ? request.getLabelType() : elevator.getLabelType();
        fields.startDate = request.getStartDate() != null ? request.getStartDate() : elevator.getLabelDate();
        fields.endDate = request.getEndDate() != null ? request.getEndDate() : elevator.getExpiryDate();
        validateLabelFields(fields);
        return fields;
    }

    private LabelFields resolveLabelFieldsForUpdate(ElevatorQrCode entity, Elevator elevator, ElevatorLabelUpdateRequest request) {
        LabelFields fields = new LabelFields();
        fields.labelType = request.getLabelType() != null
                ? request.getLabelType()
                : (entity.getLabelType() != null ? entity.getLabelType() : elevator.getLabelType());
        fields.startDate = request.getStartDate() != null
                ? request.getStartDate()
                : (entity.getStartDate() != null ? entity.getStartDate() : elevator.getLabelDate());
        fields.endDate = request.getEndDate() != null
                ? request.getEndDate()
                : (entity.getEndDate() != null ? entity.getEndDate() : elevator.getExpiryDate());
        validateLabelFields(fields);
        return fields;
    }

    private void validateLabelFields(LabelFields fields) {
        if (fields.labelType == null) {
            throw new ValidationException("labelType is required");
        }
        if (fields.startDate == null) {
            throw new ValidationException("startDate is required");
        }
        if (fields.endDate == null) {
            throw new ValidationException("endDate is required");
        }
        if (fields.endDate.isBefore(fields.startDate)) {
            throw new ValidationException("endDate cannot be before startDate");
        }
    }

    private void applyLabelFields(ElevatorQrCode qrCode, LabelFields fields, String description) {
        qrCode.setLabelType(fields.labelType);
        qrCode.setStartDate(fields.startDate);
        qrCode.setEndDate(fields.endDate);
        qrCode.setDescription(description);
    }

    private ElevatorQrCode storeAttachment(ElevatorQrCode qrCode, MultipartFile file) {
        validateAttachment(file);
        try {
            String storageKey = fileStorageService.saveFile(file, "ELEVATOR_LABEL", qrCode.getId());
            qrCode.setAttachmentStorageKey(storageKey);
            qrCode.setAttachmentOriginalFileName(trimToNull(file.getOriginalFilename()));
            qrCode.setAttachmentContentType(trimToNull(file.getContentType()));
            qrCode.setAttachmentSize(file.getSize());
            return qrCodeRepository.save(qrCode);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to save label file: " + ex.getMessage(), ex);
        }
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("file is required");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new ValidationException("file size must be less than or equal to 10MB");
        }
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class LabelFields {
        private LabelType labelType;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
