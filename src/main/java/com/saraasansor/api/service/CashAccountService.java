package com.saraasansor.api.service;

import com.saraasansor.api.dto.CashboxCreateRequest;
import com.saraasansor.api.dto.CashboxListItemResponse;
import com.saraasansor.api.dto.CashboxLookupDto;
import com.saraasansor.api.dto.CashboxPageResponse;
import com.saraasansor.api.dto.CashboxResponse;
import com.saraasansor.api.dto.CashboxUpdateRequest;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.CashAccount;
import com.saraasansor.api.repository.CashAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CashAccountService {

    private final CashAccountRepository cashAccountRepository;

    public CashAccountService(CashAccountRepository cashAccountRepository) {
        this.cashAccountRepository = cashAccountRepository;
    }

    @Transactional(readOnly = true)
    public CashboxPageResponse getCashboxes(String query, Boolean active, Pageable pageable) {
        Page<CashAccount> page = cashAccountRepository.search(normalizeNullable(query), active, pageable);
        CashboxPageResponse response = new CashboxPageResponse();
        response.setContent(page.map(CashboxListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public CashboxResponse getCashboxById(Long id) {
        CashAccount account = cashAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cashbox not found"));
        return CashboxResponse.fromEntity(account);
    }

    public CashboxResponse createCashbox(CashboxCreateRequest request) {
        validateCreateRequest(request);

        if (cashAccountRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new RuntimeException("Cashbox name already exists");
        }

        CashAccount account = new CashAccount();
        account.setName(request.getName().trim());
        account.setCurrency(resolveCurrency(request.getCurrencyCode()));
        account.setDescription(normalizeNullable(request.getDescription()));
        account.setActive(request.getActive() == null || request.getActive());
        account.setCreatedBy(resolveCurrentUsername());
        account.setUpdatedBy(resolveCurrentUsername());

        CashAccount saved = cashAccountRepository.save(account);
        return CashboxResponse.fromEntity(saved);
    }

    public CashboxResponse updateCashbox(Long id, CashboxUpdateRequest request) {
        validateUpdateRequest(request);

        CashAccount account = cashAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cashbox not found"));

        if (cashAccountRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new RuntimeException("Cashbox name already exists");
        }

        account.setName(request.getName().trim());
        account.setCurrency(resolveCurrency(request.getCurrencyCode()));
        account.setDescription(normalizeNullable(request.getDescription()));
        account.setActive(request.getActive() != null ? request.getActive() : account.getActive());
        account.setUpdatedBy(resolveCurrentUsername());

        CashAccount saved = cashAccountRepository.save(account);
        return CashboxResponse.fromEntity(saved);
    }

    public void deleteCashbox(Long id) {
        CashAccount account = cashAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cashbox not found"));
        account.setActive(false);
        account.setUpdatedBy(resolveCurrentUsername());
        cashAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<CashboxLookupDto> getCashboxLookup(String query) {
        return cashAccountRepository.findLookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(CashboxLookupDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookup(String query) {
        return cashAccountRepository.findLookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(account -> new LookupDto(account.getId(), account.getName()))
                .toList();
    }

    private void validateCreateRequest(CashboxCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (!StringUtils.hasText(request.getCurrencyCode())) {
            throw new RuntimeException("currencyCode is required");
        }
    }

    private void validateUpdateRequest(CashboxUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (!StringUtils.hasText(request.getCurrencyCode())) {
            throw new RuntimeException("currencyCode is required");
        }
    }

    private B2BCurrency resolveCurrency(String currencyCode) {
        String normalized = normalizeNullable(currencyCode);
        if (!StringUtils.hasText(normalized)) {
            throw new RuntimeException("currencyCode is required");
        }
        try {
            return B2BCurrency.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Currency not found: " + currencyCode);
        }
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
