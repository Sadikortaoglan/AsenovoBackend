package com.saraasansor.api.location.util;

import java.text.Normalizer;
import java.util.Locale;

public final class LocationNameNormalizer {

    private LocationNameNormalizer() {
    }

    public static String canonicalKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim()
                .replaceAll("\\s+", " ")
                .replace('İ', 'I')
                .replace('ı', 'i')
                .toLowerCase(Locale.ROOT);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('ö', 'o')
                .replace('ş', 's')
                .replace('ü', 'u');
    }
}
