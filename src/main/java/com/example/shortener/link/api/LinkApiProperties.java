package com.example.shortener.link.api;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Validated
@ConfigurationProperties(prefix = "app.links")
public record LinkApiProperties(@NotNull URI publicBaseUrl) {
}
