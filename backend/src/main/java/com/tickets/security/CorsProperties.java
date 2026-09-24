package com.tickets.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(@NotBlank String allowedOrigin) {

    public CorsProperties {
        allowedOrigin = allowedOrigin == null ? null : allowedOrigin.trim();
        if (allowedOrigin == null || allowedOrigin.isBlank()) {
            throw new IllegalArgumentException("app.cors.allowed-origin must be a single explicit origin");
        }
        if ("*".equals(allowedOrigin) || allowedOrigin.contains("*")
                || allowedOrigin.contains(",") || allowedOrigin.contains(" ")) {
            throw new IllegalArgumentException(
                    "app.cors.allowed-origin must be a single explicit origin; FRONTEND_ORIGIN=* is not allowed");
        }
    }
}
