package com.saraasansor.api.service;

import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.dto.WarehouseCreateRequest;
import com.saraasansor.api.dto.WarehousePageResponse;
import com.saraasansor.api.dto.WarehouseResponse;
import com.saraasansor.api.dto.WarehouseUpdateRequest;
import com.saraasansor.api.model.Warehouse;
import com.saraasansor.api.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class WarehouseService {

    private static final int MAX_NAME_LENGTH = 255;

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public WarehousePageResponse getWarehouses(String query, Boolean active, Pageable pageable) {
        Page<Warehouse> page = warehouseRepository.search(normalizeNullable(query), active, pageable);
        WarehousePageResponse response = new WarehousePageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.WarehouseListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        return WarehouseResponse.fromEntity(warehouse);
    }

    public WarehouseResponse createWarehouse(WarehouseCreateRequest request) {
        validateCreateRequest(request);
        String normalizedName = normalizeName(request.getName());
        validateNameLength(normalizedName);
        boolean targetActive = request.getActive() == null || request.getActive();

        if (targetActive && warehouseRepository.existsByNameIgnoreCaseAndActiveTrue(normalizedName)) {
            throw new RuntimeException("Warehouse name already exists");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(normalizedName);
        warehouse.setActive(targetActive);

        return WarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    public WarehouseResponse updateWarehouse(Long id, WarehouseUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedName = normalizeName(request.getName());
        validateNameLength(normalizedName);

        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        boolean targetActive = request.getActive() != null ? request.getActive() : warehouse.getActive();

        if (targetActive && warehouseRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(normalizedName, id)) {
            throw new RuntimeException("Warehouse name already exists");
        }

        warehouse.setName(normalizedName);
        warehouse.setActive(targetActive);

        return WarehouseResponse.fromEntity(warehouseRepository.save(warehouse));
    }

    public void deleteWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookup(String query) {
        return warehouseRepository.findLookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(warehouse -> new LookupDto(warehouse.getId(), warehouse.getName()))
                .toList();
    }

    private void validateCreateRequest(WarehouseCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
    }

    private void validateUpdateRequest(WarehouseUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
    }

    private void validateNameLength(String name) {
        if (name.length() > MAX_NAME_LENGTH) {
            throw new RuntimeException("name must be at most 255 characters");
        }
    }

    private String normalizeName(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
