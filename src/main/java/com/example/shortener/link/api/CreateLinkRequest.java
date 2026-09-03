package com.example.shortener.link.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) String destinationUrl,
        @Pattern(regexp = "[A-Za-z0-9_-]{4,32}", message = "must contain 4-32 URL-safe characters") String customAlias,
        Instant expiresAt) {
}
