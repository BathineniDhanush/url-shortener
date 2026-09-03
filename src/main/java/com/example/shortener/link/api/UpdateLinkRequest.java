package com.example.shortener.link.api;

import com.example.shortener.link.domain.LinkStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateLinkRequest(
        @NotNull @Min(0) Long expectedVersion,
        @Size(max = 2048) String destinationUrl,
        LinkStatus status,
        Instant expiresAt) {
}
