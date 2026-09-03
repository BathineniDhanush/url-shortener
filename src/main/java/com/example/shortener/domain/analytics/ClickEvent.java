package com.example.shortener.domain.analytics;

import java.time.Instant;
import java.util.UUID;

public record ClickEvent(
    UUID eventId,
    UUID linkId,
    Instant timestamp,
    String anonymizedIp,
    String userAgent
) {}
