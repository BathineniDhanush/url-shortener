package com.example.shortener.link.api;

import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LinkResponse(UUID id, String code, URI shortUrl, String destinationUrl,
                           LinkStatus status, Instant expiresAt, Instant createdAt, long version,
                           String ownerToken) {
    static LinkResponse from(Link link, URI publicBaseUrl, long version, String ownerToken) {
        return new LinkResponse(link.id(), link.code(), publicBaseUrl.resolve("/" + link.code()),
                link.destinationUrl(), link.status(), link.expiresAt(), link.createdAt(), version, ownerToken);
    }
}
