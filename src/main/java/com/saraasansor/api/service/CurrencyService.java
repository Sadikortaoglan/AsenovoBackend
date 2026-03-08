package com.saraasansor.api.service;

import com.saraasansor.api.dto.CurrencyOptionDto;
import com.saraasansor.api.model.B2BCurrency;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CurrencyService {

    public List<CurrencyOptionDto> getCurrencies() {
        return Arrays.stream(B2BCurrency.values())
                .map(CurrencyOptionDto::fromCurrency)
                .collect(Collectors.toList());
    }
}
