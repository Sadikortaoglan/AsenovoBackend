package com.saraasansor.api.service;

import com.saraasansor.api.dto.BankCreateRequest;
import com.saraasansor.api.dto.BankLookupDto;
import com.saraasansor.api.dto.BankPageResponse;
import com.saraasansor.api.dto.BankResponse;
import com.saraasansor.api.dto.BankUpdateRequest;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.BankAccount;
import com.saraasansor.api.repository.BankAccountRepository;
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
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional(readOnly = true)
    public BankPageResponse getBanks(String query, Boolean active, Pageable pageable) {
        Page<BankAccount> page = bankAccountRepository.search(normalizeNullable(query), active, pageable);
        BankPageResponse response = new BankPageResponse();
        response.setContent(page.map(com.saraasansor.api.dto.BankListItemResponse::fromEntity).getContent());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        return response;
    }

    @Transactional(readOnly = true)
    public BankResponse getBankById(Long id) {
        BankAccount bank = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found"));
        return BankResponse.fromEntity(bank);
    }

    public BankResponse createBank(BankCreateRequest request) {
        validateCreateRequest(request);
        if (bankAccountRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new RuntimeException("Bank name already exists");
        }

        BankAccount bank = new BankAccount();
        bank.setName(request.getName().trim());
        bank.setBranchName(normalizeNullable(request.getBranchName()));
        bank.setAccountNumber(normalizeNullable(request.getAccountNumber()));
        bank.setIban(normalizeIban(request.getIban()));
        bank.setCurrency(resolveCurrency(request.getCurrencyCode()));
        bank.setActive(request.getActive() == null || request.getActive());
        bank.setCreatedBy(resolveCurrentUsername());
        bank.setUpdatedBy(resolveCurrentUsername());

        return BankResponse.fromEntity(bankAccountRepository.save(bank));
    }

    public BankResponse updateBank(Long id, BankUpdateRequest request) {
        validateUpdateRequest(request);
        BankAccount bank = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found"));

        if (bankAccountRepository.existsByNameIgnoreCaseAndIdNot(request.getName().trim(), id)) {
            throw new RuntimeException("Bank name already exists");
        }

        bank.setName(request.getName().trim());
        bank.setBranchName(normalizeNullable(request.getBranchName()));
        bank.setAccountNumber(normalizeNullable(request.getAccountNumber()));
        bank.setIban(normalizeIban(request.getIban()));
        bank.setCurrency(resolveCurrency(request.getCurrencyCode()));
        bank.setActive(request.getActive() != null ? request.getActive() : bank.getActive());
        bank.setUpdatedBy(resolveCurrentUsername());

        return BankResponse.fromEntity(bankAccountRepository.save(bank));
    }

    public void deleteBank(Long id) {
        BankAccount bank = bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found"));
        bank.setActive(false);
        bank.setUpdatedBy(resolveCurrentUsername());
        bankAccountRepository.save(bank);
    }

    @Transactional(readOnly = true)
    public List<BankLookupDto> getBankLookup(String query) {
        return bankAccountRepository.findLookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(BankLookupDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LookupDto> getLookup(String query) {
        return bankAccountRepository.findLookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(account -> new LookupDto(account.getId(), account.getName()))
                .toList();
    }

    private void validateCreateRequest(BankCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (!StringUtils.hasText(request.getCurrencyCode())) {
            throw new RuntimeException("currencyCode is required");
        }
        validateIbanFormat(request.getIban());
    }

    private void validateUpdateRequest(BankUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new RuntimeException("name is required");
        }
        if (!StringUtils.hasText(request.getCurrencyCode())) {
            throw new RuntimeException("currencyCode is required");
        }
        validateIbanFormat(request.getIban());
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

    private void validateIbanFormat(String iban) {
        if (!StringUtils.hasText(iban)) {
            return;
        }
        String normalized = normalizeIban(iban);
        if (!normalized.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{8,30}")) {
            throw new RuntimeException("Invalid iban format");
        }
    }

    private String normalizeIban(String iban) {
        if (!StringUtils.hasText(iban)) {
            return null;
        }
        return iban.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
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
