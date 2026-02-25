package com.saraasansor.api.service;

import com.saraasansor.api.dto.EdmSettingDto;
import com.saraasansor.api.model.EdmSetting;
import com.saraasansor.api.repository.EdmSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EdmSettingService {
    @Autowired
    private EdmSettingRepository repository;

    @Autowired
    private SensitiveDataCryptoService cryptoService;

    public EdmSettingDto getCurrent() {
        EdmSetting entity = repository.findAll().stream().findFirst().orElse(null);
        if (entity == null) {
            return null;
        }
        EdmSettingDto dto = new EdmSettingDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setInvoiceSeriesEarchive(entity.getInvoiceSeriesEarchive());
        dto.setInvoiceSeriesEfatura(entity.getInvoiceSeriesEfatura());
        dto.setMode(entity.getMode());
        dto.setPasswordConfigured(entity.getEncryptedPassword() != null && !entity.getEncryptedPassword().isBlank());
        return dto;
    }

    public EdmSettingDto save(EdmSettingDto dto) {
        EdmSetting entity = repository.findAll().stream().findFirst().orElse(new EdmSetting());
        entity.setUsername(dto.getUsername());
        entity.setEmail(dto.getEmail());
        entity.setInvoiceSeriesEarchive(dto.getInvoiceSeriesEarchive());
        entity.setInvoiceSeriesEfatura(dto.getInvoiceSeriesEfatura());
        entity.setMode(dto.getMode() == null ? "PRODUCTION" : dto.getMode());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            entity.setEncryptedPassword(cryptoService.encrypt(dto.getPassword()));
        } else if (entity.getEncryptedPassword() == null) {
            throw new RuntimeException("EDM password is required for initial setup");
        }

        repository.save(entity);
        return getCurrent();
    }
}
