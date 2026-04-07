package com.saraasansor.api.service;

import com.saraasansor.api.dto.VatRateCreateRequest;
import com.saraasansor.api.dto.VatRateLookupDto;
import com.saraasansor.api.dto.VatRatePageResponse;
import com.saraasansor.api.dto.VatRateResponse;
import com.saraasansor.api.dto.VatRateUpdateRequest;
import com.saraasansor.api.model.VatRate;
import com.saraasansor.api.repository.VatRateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class VatRateService {

    private final VatRateRepository vatRateRepository;

    public VatRateService(VatRateRepository vatRateRepository) {
        this.vatRateRepository = vatRateRepository;
    }

    @Transactional(readOnly = true)
    public VatRatePageResponse getVatRates(String query, Boolean active, Pageable pageable) {
        Page<VatRate> page = vatRateRepository.search(normalizeNullable(query), active, pageable);
        VatRatePageResponse response = new VatRatePageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.VatRateListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public VatRateResponse getVatRateById(Long id) {
        VatRate vatRate = vatRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VAT rate not found"));
        return VatRateResponse.fromEntity(vatRate);
    }

    public VatRateResponse createVatRate(VatRateCreateRequest request) {
        validateCreateRequest(request);
        BigDecimal normalizedRate = normalizeRate(request.getRate());
        VatRate vatRate = vatRateRepository.getVatRateByRate(normalizedRate);
        if (vatRate != null && vatRate.getActive()) {
            throw new RuntimeException("VAT rate already exists");
        }else if (vatRate != null && !vatRate.getActive()) {
            vatRate.setActive(true);
            vatRate.setUpdatedBy(resolveCurrentUsername());
            vatRateRepository.save(vatRate);
            return VatRateResponse.fromEntity(vatRate);
        }

        vatRate = new VatRate();
        vatRate.setRate(normalizedRate);
        vatRate.setActive(request.getActive() == null || request.getActive());
        vatRate.setCreatedBy(resolveCurrentUsername());
        vatRate.setUpdatedBy(resolveCurrentUsername());

        return VatRateResponse.fromEntity(vatRateRepository.save(vatRate));
    }

    public VatRateResponse updateVatRate(Long id, VatRateUpdateRequest request) {
        validateUpdateRequest(request);
        BigDecimal normalizedRate = normalizeRate(request.getRate());

        VatRate vatRate = vatRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VAT rate not found"));

        if (vatRateRepository.existsByRateAndIdNot(normalizedRate, id)) {
            throw new RuntimeException("VAT rate already exists");
        }

        vatRate.setRate(normalizedRate);
        vatRate.setActive(request.getActive() != null ? request.getActive() : vatRate.getActive());
        vatRate.setUpdatedBy(resolveCurrentUsername());
        return VatRateResponse.fromEntity(vatRateRepository.save(vatRate));
    }

    public void deleteVatRate(Long id) {
        VatRate vatRate = vatRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VAT rate not found"));
        vatRate.setActive(false);
        vatRate.setUpdatedBy(resolveCurrentUsername());
        vatRateRepository.save(vatRate);
    }

    @Transactional(readOnly = true)
    public List<VatRateLookupDto> getLookup(String query) {
        return vatRateRepository.lookup(normalizeNullable(query), PageRequest.of(0, 200))
                .stream()
                .map(VatRateLookupDto::fromEntity)
                .toList();
    }

    private void validateCreateRequest(VatRateCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        validateRate(request.getRate());
    }

    private void validateUpdateRequest(VatRateUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        validateRate(request.getRate());
    }

    private void validateRate(BigDecimal rate) {
        if (rate == null) {
            throw new RuntimeException("rate is required");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("rate cannot be negative");
        }
        if (rate.compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException("rate must be less than or equal to 100");
        }
    }

    private BigDecimal normalizeRate(BigDecimal rate) {
        return rate.setScale(2, RoundingMode.HALF_UP);
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
