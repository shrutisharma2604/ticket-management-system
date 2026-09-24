package com.tickets.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security")
public record SecurityProperties(
        @NotBlank String jwtSecret,
        @NotBlank String operatorUsername,
        @NotBlank String operatorPassword,
        @NotNull Duration tokenTtl) {
}
