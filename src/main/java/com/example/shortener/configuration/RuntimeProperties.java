package com.example.shortener.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.runtime")
public record RuntimeProperties(@NotNull RuntimeRole role) {
}
