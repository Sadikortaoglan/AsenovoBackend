package com.saraasansor.api.service;

import com.saraasansor.api.dto.StockGroupCreateRequest;
import com.saraasansor.api.dto.StockGroupPageResponse;
import com.saraasansor.api.dto.StockGroupResponse;
import com.saraasansor.api.dto.StockGroupUpdateRequest;
import com.saraasansor.api.model.StockGroup;
import com.saraasansor.api.repository.StockGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class StockGroupService {

    private static final int MAX_NAME_LENGTH = 255;

    private final StockGroupRepository stockGroupRepository;

    public StockGroupService(StockGroupRepository stockGroupRepository) {
        this.stockGroupRepository = stockGroupRepository;
    }

    @Transactional(readOnly = true)
    public StockGroupPageResponse getStockGroups(String query, Boolean active, Pageable pageable) {
        Page<StockGroup> page = stockGroupRepository.search(normalizeNullable(query), active, pageable);
        StockGroupPageResponse response = new StockGroupPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.StockGroupListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public StockGroupResponse getStockGroupById(Long id) {
        StockGroup stockGroup = stockGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock group not found"));
        return StockGroupResponse.fromEntity(stockGroup);
    }

    public StockGroupResponse createStockGroup(StockGroupCreateRequest request) {
        validateCreateRequest(request);
        String normalizedName = normalizeName(request.getName());
        validateNameLength(normalizedName);
        boolean targetActive = request.getActive() == null || request.getActive();

        if (targetActive && stockGroupRepository.existsByNameIgnoreCaseAndActiveTrue(normalizedName)) {
            throw new RuntimeException("Stock group name already exists");
        }

        StockGroup stockGroup = new StockGroup();
        stockGroup.setName(normalizedName);
        stockGroup.setActive(targetActive);
        stockGroup.setCreatedBy(resolveCurrentUsername());
        stockGroup.setUpdatedBy(resolveCurrentUsername());

        return StockGroupResponse.fromEntity(stockGroupRepository.save(stockGroup));
    }

    public StockGroupResponse updateStockGroup(Long id, StockGroupUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedName = normalizeName(request.getName());
        validateNameLength(normalizedName);

        StockGroup stockGroup = stockGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock group not found"));
        boolean targetActive = request.getActive() != null ? request.getActive() : stockGroup.getActive();

        if (targetActive && stockGroupRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(normalizedName, id)) {
            throw new RuntimeException("Stock group name already exists");
        }

        stockGroup.setName(normalizedName);
        stockGroup.setActive(targetActive);
        stockGroup.setUpdatedBy(resolveCurrentUsername());

        return StockGroupResponse.fromEntity(stockGroupRepository.save(stockGroup));
    }

    public void deleteStockGroup(Long id) {
        StockGroup stockGroup = stockGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock group not found"));
        stockGroup.setActive(false);
        stockGroup.setUpdatedBy(resolveCurrentUsername());
        stockGroupRepository.save(stockGroup);
    }

    private void validateCreateRequest(StockGroupCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
    }

    private void validateUpdateRequest(StockGroupUpdateRequest request) {
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
