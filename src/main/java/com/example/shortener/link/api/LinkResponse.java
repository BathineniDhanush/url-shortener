package com.example.shortener.link.api;

import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkStatus;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record LinkResponse(UUID id, String code, URI shortUrl, String destinationUrl,
                           LinkStatus status, Instant expiresAt, Instant createdAt) {
    static LinkResponse from(Link link, URI publicBaseUrl) {
        return new LinkResponse(link.id(), link.code(), publicBaseUrl.resolve("/" + link.code()),
                link.destinationUrl(), link.status(), link.expiresAt(), link.createdAt());
    }
}
