package com.example.shortener.link.domain;

import java.time.Instant;
import java.util.UUID;

public record Link(UUID id, String code, String destinationUrl, LinkStatus status,
                   Instant expiresAt, Instant createdAt, Instant updatedAt) {
}
