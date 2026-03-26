package com.fatih.pharmacyfinder.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
public class ValidationService {

    private static final Pattern CITY_DISTRICT_PATTERN = Pattern.compile("^[a-zA-ZğüşıöçĞÜŞİÖÇ\\s]{2,50}$");

    public boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    public boolean isValidCityOrDistrict(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String trimmed = value.trim();
        return CITY_DISTRICT_PATTERN.matcher(trimmed).matches();
    }

    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                .replaceAll("[<>\"'&]", "") // Remove potential XSS characters
                .substring(0, Math.min(input.length(), 100)); // Limit length
    }
}