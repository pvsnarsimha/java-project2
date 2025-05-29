package com.example.bloodsugartracker.util;

import org.springframework.stereotype.Component;

@Component("stringUtils")
public class StringUtils {
    public String sanitizeId(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "unknown";
        }
        return str.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}