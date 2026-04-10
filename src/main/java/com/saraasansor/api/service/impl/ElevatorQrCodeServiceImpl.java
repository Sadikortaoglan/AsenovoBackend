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
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorQrCode;
import com.saraasansor.api.repository.ElevatorQrCodeRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.service.ElevatorQrService;
import com.saraasansor.api.service.ElevatorQrCodeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ElevatorQrCodeServiceImpl implements ElevatorQrCodeService {

    private final ElevatorQrCodeRepository qrCodeRepository;
    private final ElevatorRepository elevatorRepository;
    private final ElevatorQrService elevatorQrService;

    public ElevatorQrCodeServiceImpl(ElevatorQrCodeRepository qrCodeRepository,
                                     ElevatorRepository elevatorRepository,
                                     ElevatorQrService elevatorQrService) {
        this.qrCodeRepository = qrCodeRepository;
        this.elevatorRepository = elevatorRepository;
        this.elevatorQrService = elevatorQrService;
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
        Elevator elevator = resolveElevator(request != null ? request.getElevatorId() : null);

        ElevatorQrCode qrCode = new ElevatorQrCode();
        qrCode.setUuid(UUID.randomUUID());
        qrCode.setElevator(elevator);
        qrCode.setQrValue(generateUniqueQrValue());
        qrCode.setCompanyId(companyId);

        return toDto(qrCodeRepository.save(qrCode));
    }

    @Override
    public QrCodeResponseDTO update(Long id, ElevatorLabelUpdateRequest request, Long companyId) {
        ElevatorQrCode qrCode = findByIdAndCompanyId(id, companyId);
        Elevator elevator = resolveElevator(request != null ? request.getElevatorId() : null);
        qrCode.setElevator(elevator);
        return toDto(qrCodeRepository.save(qrCode));
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
            throw new RuntimeException("elevatorId is required");
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
}
