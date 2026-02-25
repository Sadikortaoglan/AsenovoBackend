package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorLabelDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorLabel;
import com.saraasansor.api.repository.ElevatorLabelRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ElevatorLabelService {
    @Autowired
    private ElevatorLabelRepository repository;

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Page<ElevatorLabelDto> list(Long elevatorId, Pageable pageable) {
        Page<ElevatorLabel> page = elevatorId == null
                ? repository.findAll(pageable)
                : repository.findByElevatorId(elevatorId, pageable);
        return page.map(ElevatorLabelDto::fromEntity);
    }

    public ElevatorLabelDto create(ElevatorLabelDto dto, MultipartFile file) {
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new NotFoundException("Elevator not found"));

        ElevatorLabel entity = new ElevatorLabel();
        entity.setElevator(elevator);
        entity.setLabelName(dto.getLabelName());
        entity.setStartAt(dto.getStartAt());
        entity.setEndAt(dto.getEndAt());
        entity.setDescription(dto.getDescription());

        if (file != null && !file.isEmpty()) {
            try {
                String key = fileStorageService.saveFile(file, "elevator-label", elevator.getId());
                entity.setFilePath(fileStorageService.getFileUrl(key));
            } catch (Exception e) {
                throw new RuntimeException("Label file upload failed");
            }
        }

        return ElevatorLabelDto.fromEntity(repository.save(entity));
    }

    public ElevatorLabelDto update(Long id, ElevatorLabelDto dto, MultipartFile file) {
        ElevatorLabel entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Label not found"));

        if (dto.getLabelName() != null) entity.setLabelName(dto.getLabelName());
        if (dto.getStartAt() != null) entity.setStartAt(dto.getStartAt());
        if (dto.getEndAt() != null) entity.setEndAt(dto.getEndAt());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());

        if (file != null && !file.isEmpty()) {
            try {
                String key = fileStorageService.saveFile(file, "elevator-label", entity.getElevator().getId());
                entity.setFilePath(fileStorageService.getFileUrl(key));
            } catch (Exception e) {
                throw new RuntimeException("Label file upload failed");
            }
        }

        return ElevatorLabelDto.fromEntity(repository.save(entity));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Label not found");
        }
        repository.deleteById(id);
    }
}
