package com.saraasansor.api.service;

import com.saraasansor.api.dto.StatusDetectionReportDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.StatusDetectionReport;
import com.saraasansor.api.repository.StatusDetectionReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@Transactional
public class StatusDetectionReportService {
    @Autowired
    private StatusDetectionReportRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    public Page<StatusDetectionReportDto> list(LocalDate start, LocalDate end, String building, String status, Pageable pageable) {
        LocalDate from = start == null ? LocalDate.now().minusYears(1) : start;
        LocalDate to = end == null ? LocalDate.now().plusDays(1) : end;
        String buildingFilter = building == null ? "" : building;
        String statusFilter = status == null ? "" : status;

        return repository.findByReportDateBetweenAndBuildingNameContainingIgnoreCaseAndStatusContainingIgnoreCase(
                from, to, buildingFilter, statusFilter, pageable).map(StatusDetectionReportDto::fromEntity);
    }

    public StatusDetectionReportDto create(StatusDetectionReportDto dto, MultipartFile file) {
        StatusDetectionReport entity = new StatusDetectionReport();
        map(entity, dto);

        if (file != null && !file.isEmpty()) {
            try {
                String key = fileStorageService.saveFile(file, "status-report", 0L);
                entity.setFilePath(fileStorageService.getFileUrl(key));
            } catch (Exception e) {
                throw new RuntimeException("Report file upload failed");
            }
        }

        return StatusDetectionReportDto.fromEntity(repository.save(entity));
    }

    public StatusDetectionReportDto update(Long id, StatusDetectionReportDto dto, MultipartFile file) {
        StatusDetectionReport entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        map(entity, dto);

        if (file != null && !file.isEmpty()) {
            try {
                String key = fileStorageService.saveFile(file, "status-report", id);
                entity.setFilePath(fileStorageService.getFileUrl(key));
            } catch (Exception e) {
                throw new RuntimeException("Report file upload failed");
            }
        }

        return StatusDetectionReportDto.fromEntity(repository.save(entity));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Report not found");
        }
        repository.deleteById(id);
    }

    private void map(StatusDetectionReport entity, StatusDetectionReportDto dto) {
        entity.setReportDate(dto.getReportDate());
        entity.setBuildingName(dto.getBuildingName());
        entity.setElevatorName(dto.getElevatorName());
        entity.setIdentityNumber(dto.getIdentityNumber());
        entity.setStatus(dto.getStatus() == null ? "DRAFT" : dto.getStatus());
        entity.setNote(dto.getNote());
    }
}
