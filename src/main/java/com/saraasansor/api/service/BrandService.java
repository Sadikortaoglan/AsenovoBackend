package com.saraasansor.api.service;

import com.saraasansor.api.dto.BrandCreateRequest;
import com.saraasansor.api.dto.BrandLookupDto;
import com.saraasansor.api.dto.BrandPageResponse;
import com.saraasansor.api.dto.BrandResponse;
import com.saraasansor.api.dto.BrandUpdateRequest;
import com.saraasansor.api.model.Brand;
import com.saraasansor.api.repository.BrandRepository;
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
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public BrandPageResponse getBrands(String query, Boolean active, Pageable pageable) {
        Page<Brand> page = brandRepository.search(normalizeNullable(query), active, pageable);
        BrandPageResponse response = new BrandPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.BrandListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        return BrandResponse.fromEntity(brand);
    }

    public BrandResponse createBrand(BrandCreateRequest request) {
        validateCreateRequest(request);
        String normalizedName = normalizeName(request.getName());
        if (brandRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new RuntimeException("Brand name already exists");
        }

        Brand brand = new Brand();
        brand.setName(normalizedName);
        brand.setActive(request.getActive() == null || request.getActive());
        brand.setCreatedBy(resolveCurrentUsername());
        brand.setUpdatedBy(resolveCurrentUsername());

        return BrandResponse.fromEntity(brandRepository.save(brand));
    }

    public BrandResponse updateBrand(Long id, BrandUpdateRequest request) {
        validateUpdateRequest(request);
        String normalizedName = normalizeName(request.getName());

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));

        if (brandRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new RuntimeException("Brand name already exists");
        }

        brand.setName(normalizedName);
        brand.setActive(request.getActive() != null ? request.getActive() : brand.getActive());
        brand.setUpdatedBy(resolveCurrentUsername());

        return BrandResponse.fromEntity(brandRepository.save(brand));
    }

    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found"));
        brand.setActive(false);
        brand.setUpdatedBy(resolveCurrentUsername());
        brandRepository.save(brand);
    }

    @Transactional(readOnly = true)
    public List<BrandLookupDto> getLookup(String query) {
        return brandRepository.lookup(normalizeNullable(query), PageRequest.of(0, 200))
                .stream()
                .map(BrandLookupDto::fromEntity)
                .toList();
    }

    private void validateCreateRequest(BrandCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
    }

    private void validateUpdateRequest(BrandUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
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
