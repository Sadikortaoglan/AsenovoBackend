package com.saraasansor.api.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
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
        return qrCodeRepository.findAllBySearchAndCompanyId(search, companyId, pageable).map(this::toDto);
    }

    @Override
    public QrCodeResponseDTO create(Long elevatorId, Long companyId) {
        Elevator elevator = elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new QrCodeNotFoundException("Elevator not found: " + elevatorId));

        ElevatorQrCode qrCode = new ElevatorQrCode();
        qrCode.setUuid(UUID.randomUUID());
        qrCode.setElevator(elevator);
        qrCode.setQrValue(generateUniqueQrValue());
        qrCode.setCompanyId(companyId);

        return toDto(qrCodeRepository.save(qrCode));
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
        ElevatorQrCode qrCode = qrCodeRepository.findById(id)
                .orElseThrow(() -> new QrCodeNotFoundException("QR code not found: " + id));
        if (!qrCode.getCompanyId().equals(companyId)) {
            throw new QrCodeNotFoundException("QR code not found: " + id);
        }
        return qrCode;
    }

    private QrCodeResponseDTO toDto(ElevatorQrCode entity) {
        QrCodeResponseDTO dto = new QrCodeResponseDTO();
        dto.setId(entity.getId());
        dto.setUuid(entity.getUuid());
        dto.setElevatorId(entity.getElevator().getId());
        dto.setElevatorName(entity.getElevator().getElevatorNumber());
        dto.setBuildingName(entity.getElevator().getBuildingName());
        dto.setCustomerName(entity.getElevator().getManagerName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setHasQr(true);
        String qrPayload = buildPublicQrPayload(entity.getElevator().getId());
        dto.setQrPngBase64(Base64.getEncoder().encodeToString(generateQrImageFromValue(qrPayload, 120)));
        return dto;
    }

    private QrCodeResponseDTO toDto(ElevatorQrCodeRepository.ElevatorQrListProjection projection) {
        QrCodeResponseDTO dto = new QrCodeResponseDTO();
        dto.setId(projection.getQrId());
        dto.setUuid(projection.getUuid());
        dto.setElevatorId(projection.getElevatorId());
        dto.setElevatorName(projection.getElevatorName());
        dto.setBuildingName(projection.getBuildingName());
        dto.setCustomerName(projection.getCustomerName());
        dto.setCreatedAt(projection.getCreatedAt());
        boolean hasQr = projection.getQrId() != null && projection.getQrValue() != null;
        dto.setHasQr(hasQr);
        if (hasQr) {
            String qrPayload = buildPublicQrPayload(projection.getElevatorId());
            dto.setQrPngBase64(Base64.getEncoder().encodeToString(generateQrImageFromValue(qrPayload, 120)));
        } else {
            dto.setQrPngBase64(null);
        }
        return dto;
    }

    private String buildPublicQrPayload(Long elevatorId) {
        return elevatorQrService.generateQrUrl(elevatorId);
    }
}
