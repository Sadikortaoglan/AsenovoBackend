package com.saraasansor.api.service;

import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.repository.CashAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CashAccountService {

    private final CashAccountRepository cashAccountRepository;

    public CashAccountService(CashAccountRepository cashAccountRepository) {
        this.cashAccountRepository = cashAccountRepository;
    }

    public List<LookupDto> getLookup(String query) {
        return cashAccountRepository.findLookup(normalizeNullable(query), PageRequest.of(0, 200, Sort.by(Sort.Direction.ASC, "name")))
                .stream()
                .map(account -> new LookupDto(account.getId(), account.getName()))
                .toList();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
