package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorContractDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.ElevatorContract;
import com.saraasansor.api.repository.ElevatorContractRepository;
import com.saraasansor.api.repository.ElevatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class ElevatorContractService {
    @Autowired
    private ElevatorContractRepository repository;

    @Autowired
    private ElevatorRepository elevatorRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Page<ElevatorContractDto> list(Long elevatorId, Pageable pageable) {
        Page<ElevatorContract> page = elevatorId == null
                ? repository.findAll(pageable)
                : repository.findByElevatorId(elevatorId, pageable);
        return page.map(ElevatorContractDto::fromEntity);
    }

    public ElevatorContractDto create(ElevatorContractDto dto, MultipartFile file) {
        Elevator elevator = elevatorRepository.findById(dto.getElevatorId())
                .orElseThrow(() -> new NotFoundException("Elevator not found"));

        ElevatorContract entity = new ElevatorContract();
        entity.setElevator(elevator);
        entity.setContractDate(dto.getContractDate());
        entity.setContractHtml(dto.getContractHtml());

        if (file != null && !file.isEmpty()) {
            try {
                String key = fileStorageService.saveFile(file, "elevator-contract", elevator.getId());
                entity.setFilePath(fileStorageService.getFileUrl(key));
            } catch (Exception e) {
                throw new RuntimeException("Contract file upload failed");
            }
        }

        return ElevatorContractDto.fromEntity(repository.save(entity));
    }

    public ElevatorContractDto update(Long id, ElevatorContractDto dto, MultipartFile file) {
        ElevatorContract entity = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract not found"));

        if (dto.getContractDate() != null) entity.setContractDate(dto.getContractDate());
        if (dto.getContractHtml() != null) entity.setContractHtml(dto.getContractHtml());

        if (file != null && !file.isEmpty()) {
            try {
                String key = fileStorageService.saveFile(file, "elevator-contract", entity.getElevator().getId());
                entity.setFilePath(fileStorageService.getFileUrl(key));
            } catch (Exception e) {
                throw new RuntimeException("Contract file upload failed");
            }
        }

        return ElevatorContractDto.fromEntity(repository.save(entity));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Contract not found");
        }
        repository.deleteById(id);
    }
}
