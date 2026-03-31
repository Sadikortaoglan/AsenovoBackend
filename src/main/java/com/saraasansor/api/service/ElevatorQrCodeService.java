package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import com.saraasansor.api.dto.QrCodeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ElevatorQrCodeService {

    Page<QrCodeResponseDTO> list(Pageable pageable, String search, Long companyId);
    Page<QrCodeResponseDTO> list(Pageable pageable, String search, Long companyId, boolean onlyWithQr);

    QrCodeResponseDTO create(Long elevatorId, Long companyId);

    QrCodeResponseDTO create(ElevatorLabelCreateRequest request, Long companyId);

    QrCodeResponseDTO update(Long id, ElevatorLabelUpdateRequest request, Long companyId);

    QrCodeResponseDTO getById(Long id, Long companyId);

    void delete(Long id, Long companyId);

    byte[] generateQrImage(Long id, Long companyId);
}
