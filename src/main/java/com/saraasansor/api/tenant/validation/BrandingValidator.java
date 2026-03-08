package com.saraasansor.api.tenant.validation;

import com.saraasansor.api.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class BrandingValidator {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6})$");
    private static final String BLACK = "#000000";
    private static final String WHITE = "#FFFFFF";

    public void validateColorsForUpdate(String primaryColor, String secondaryColor) {
        validateColor("primaryColor", primaryColor);
        validateColor("secondaryColor", secondaryColor);
    }

    private void validateColor(String fieldName, String color) {
        if (color == null) {
            throw new ValidationException(fieldName + " cannot be null");
        }

        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new ValidationException(fieldName + " must be a valid hex color in format #RRGGBB");
        }

        String normalized = color.toUpperCase(Locale.ROOT);
        if (BLACK.equals(normalized) || WHITE.equals(normalized)) {
            throw new ValidationException(fieldName + " cannot be pure black or pure white");
        }
    }
}
