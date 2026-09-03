package com.example.shortener.link.application;

import java.time.Instant;

public record CreateLinkCommand(String destinationUrl, String customAlias, Instant expiresAt) {
}
