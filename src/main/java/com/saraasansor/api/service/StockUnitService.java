package com.saraasansor.api.service;

import com.saraasansor.api.dto.StockUnitCreateRequest;
import com.saraasansor.api.dto.StockUnitLookupDto;
import com.saraasansor.api.dto.StockUnitPageResponse;
import com.saraasansor.api.dto.StockUnitResponse;
import com.saraasansor.api.dto.StockUnitUpdateRequest;
import com.saraasansor.api.model.StockUnit;
import com.saraasansor.api.repository.StockUnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class StockUnitService {

    private final StockUnitRepository stockUnitRepository;

    public StockUnitService(StockUnitRepository stockUnitRepository) {
        this.stockUnitRepository = stockUnitRepository;
    }

    @Transactional(readOnly = true)
    public StockUnitPageResponse getStockUnits(String query, Boolean active, Pageable pageable) {
        Page<StockUnit> page = stockUnitRepository.search(normalizeNullable(query), active, pageable);
        StockUnitPageResponse response = new StockUnitPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.StockUnitListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public StockUnitResponse getStockUnitById(Long id) {
        StockUnit stockUnit = stockUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock unit not found"));
        return StockUnitResponse.fromEntity(stockUnit);
    }

    public StockUnitResponse createStockUnit(StockUnitCreateRequest request) {
        validateCreateRequest(request);
        String normalizedName = normalizeValue(request.getName());
        String normalizedAbbreviation = normalizeValue(request.getAbbreviation());

        if (stockUnitRepository.existsByNameIgnoreCaseAndActiveTrue(normalizedName)) {
            throw new RuntimeException("Stock unit name already exists");
        }
        if (stockUnitRepository.existsByAbbreviationIgnoreCaseAndActiveTrue(normalizedAbbreviation)) {
            throw new RuntimeException("Stock unit abbreviation already exists");
        }

        StockUnit stockUnit = new StockUnit();
        stockUnit.setName(normalizedName);
        stockUnit.setAbbreviation(normalizedAbbreviation);
        stockUnit.setActive(request.getActive() == null || request.getActive());
        stockUnit.setCreatedBy(resolveCurrentUsername());
        stockUnit.setUpdatedBy(resolveCurrentUsername());

        return StockUnitResponse.fromEntity(stockUnitRepository.save(stockUnit));
    }

    public StockUnitResponse updateStockUnit(Long id, StockUnitUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedName = normalizeValue(request.getName());
        String normalizedAbbreviation = normalizeValue(request.getAbbreviation());

        StockUnit stockUnit = stockUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock unit not found"));

        if (stockUnitRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(normalizedName, id)) {
            throw new RuntimeException("Stock unit name already exists");
        }
        if (stockUnitRepository.existsByAbbreviationIgnoreCaseAndIdNotAndActiveTrue(normalizedAbbreviation, id)) {
            throw new RuntimeException("Stock unit abbreviation already exists");
        }

        stockUnit.setName(normalizedName);
        stockUnit.setAbbreviation(normalizedAbbreviation);
        stockUnit.setActive(request.getActive() != null ? request.getActive() : stockUnit.getActive());
        stockUnit.setUpdatedBy(resolveCurrentUsername());

        return StockUnitResponse.fromEntity(stockUnitRepository.save(stockUnit));
    }

    public void deleteStockUnit(Long id) {
        StockUnit stockUnit = stockUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock unit not found"));
        stockUnit.setActive(false);
        stockUnit.setUpdatedBy(resolveCurrentUsername());
        stockUnitRepository.save(stockUnit);
    }

    @Transactional(readOnly = true)
    public List<StockUnitLookupDto> getLookup(String query) {
        return stockUnitRepository.lookup(normalizeNullable(query), PageRequest.of(0, 200))
                .stream()
                .map(StockUnitLookupDto::fromEntity)
                .toList();
    }

    private void validateCreateRequest(StockUnitCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (!StringUtils.hasText(request.getAbbreviation())) {
            throw new RuntimeException("abbreviation is required");
        }
    }

    private void validateUpdateRequest(StockUnitUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (!StringUtils.hasText(request.getAbbreviation())) {
            throw new RuntimeException("abbreviation is required");
        }
    }

    private String normalizeValue(String value) {
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String principalName && !"anonymousUser".equals(principalName)) {
            return principalName;
        }
        return null;
    }
}
