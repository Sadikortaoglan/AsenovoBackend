package com.saraasansor.api.service;

import com.saraasansor.api.dto.QrCodeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ElevatorQrCodeService {

    Page<QrCodeResponseDTO> list(Pageable pageable, String search, Long companyId);

    QrCodeResponseDTO create(Long elevatorId, Long companyId);

    void delete(Long id, Long companyId);

    byte[] generateQrImage(Long id, Long companyId);
}
