package com.saraasansor.api.service;

import com.saraasansor.api.dto.EInvoiceQueryResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EInvoiceService {

    public EInvoiceQueryResponseDto queryByTaxNumber(String taxNumber) {
        if (!StringUtils.hasText(taxNumber)) {
            throw new RuntimeException("taxNumber is required");
        }

        String normalized = taxNumber.trim();
        if (!normalized.matches("^(\\d{10}|\\d{11})$")) {
            throw new RuntimeException("Tax number must be 10 or 11 digits");
        }

        int lastDigit = Character.getNumericValue(normalized.charAt(normalized.length() - 1));
        boolean eInvoiceUser = lastDigit % 2 == 0;
        String message = eInvoiceUser
                ? "Stub response: Tax number appears as e-invoice user."
                : "Stub response: Tax number appears as non e-invoice user.";

        return new EInvoiceQueryResponseDto(eInvoiceUser, message);
    }
}
