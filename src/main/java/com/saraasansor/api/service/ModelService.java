package com.saraasansor.api.service;

import com.saraasansor.api.dto.ModelCreateRequest;
import com.saraasansor.api.dto.ModelLookupDto;
import com.saraasansor.api.dto.ModelPageResponse;
import com.saraasansor.api.dto.ModelResponse;
import com.saraasansor.api.dto.ModelUpdateRequest;
import com.saraasansor.api.model.Brand;
import com.saraasansor.api.model.StockModel;
import com.saraasansor.api.repository.BrandRepository;
import com.saraasansor.api.repository.StockModelRepository;
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
public class ModelService {

    private final StockModelRepository stockModelRepository;
    private final BrandRepository brandRepository;

    public ModelService(StockModelRepository stockModelRepository, BrandRepository brandRepository) {
        this.stockModelRepository = stockModelRepository;
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public ModelPageResponse getModels(String query, Boolean active, Pageable pageable) {
        Page<StockModel> page = stockModelRepository.search(normalizeNullable(query), active, pageable);
        ModelPageResponse response = new ModelPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.ModelListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public ModelResponse getModelById(Long id) {
        StockModel model = stockModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found"));
        return ModelResponse.fromEntity(model);
    }

    public ModelResponse createModel(ModelCreateRequest request) {
        validateCreateRequest(request);
        String normalizedName = normalizeName(request.getName());
        Brand brand = findActiveBrand(request.getBrandId());

        if (stockModelRepository.existsByBrandIdAndNameIgnoreCaseAndActiveTrue(brand.getId(), normalizedName)) {
            throw new RuntimeException("Model name already exists for selected brand");
        }

        StockModel model = new StockModel();
        model.setName(normalizedName);
        model.setBrand(brand);
        model.setActive(request.getActive() == null || request.getActive());
        model.setCreatedBy(resolveCurrentUsername());
        model.setUpdatedBy(resolveCurrentUsername());

        return ModelResponse.fromEntity(stockModelRepository.save(model));
    }

    public ModelResponse updateModel(Long id, ModelUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedName = normalizeName(request.getName());
        StockModel model = stockModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found"));
        Brand brand = findActiveBrand(request.getBrandId());

        if (stockModelRepository.existsByBrandIdAndNameIgnoreCaseAndIdNotAndActiveTrue(brand.getId(), normalizedName, id)) {
            throw new RuntimeException("Model name already exists for selected brand");
        }

        model.setName(normalizedName);
        model.setBrand(brand);
        model.setActive(request.getActive() != null ? request.getActive() : model.getActive());
        model.setUpdatedBy(resolveCurrentUsername());

        return ModelResponse.fromEntity(stockModelRepository.save(model));
    }

    public void deleteModel(Long id) {
        StockModel model = stockModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found"));
        model.setActive(false);
        model.setUpdatedBy(resolveCurrentUsername());
        stockModelRepository.save(model);
    }

    @Transactional(readOnly = true)
    public List<ModelLookupDto> getLookup(String query) {
        return stockModelRepository.lookup(normalizeNullable(query), PageRequest.of(0, 200))
                .stream()
                .map(ModelLookupDto::fromEntity)
                .toList();
    }

    private Brand findActiveBrand(Long brandId) {
        if (brandId == null) {
            throw new RuntimeException("brandId is required");
        }
        return brandRepository.findByIdAndActiveTrue(brandId)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
    }

    private void validateCreateRequest(ModelCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (request.getBrandId() == null) {
            throw new RuntimeException("brandId is required");
        }
    }

    private void validateUpdateRequest(ModelUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (request.getBrandId() == null) {
            throw new RuntimeException("brandId is required");
        }
    }

    private String normalizeName(String name) {
        return name.trim();
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

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
