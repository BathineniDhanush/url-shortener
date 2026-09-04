package com.example.shortener.link.application;

import com.example.shortener.infrastructure.cache.LinkCache;
import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.DestinationUrlPolicy;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.domain.OwnedLink;
import com.example.shortener.link.error.ConcurrentLinkUpdateException;
import com.example.shortener.link.error.InvalidExpirationException;
import com.example.shortener.link.error.InvalidLinkUpdateException;
import com.example.shortener.link.error.LinkNotFoundException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

@Service
public class ManageLinkService {
    private static final Logger log = LoggerFactory.getLogger(ManageLinkService.class);
    private final LinkRepository linkRepository;
    private final OwnerTokenService ownerTokenService;
    private final DestinationUrlPolicy destinationUrlPolicy;
    private final LinkCache linkCache;
    private final Clock clock;

    public ManageLinkService(LinkRepository linkRepository, OwnerTokenService ownerTokenService,
                             DestinationUrlPolicy destinationUrlPolicy, LinkCache linkCache, Clock clock) {
        this.linkRepository = linkRepository;
        this.ownerTokenService = ownerTokenService;
        this.destinationUrlPolicy = destinationUrlPolicy;
        this.linkCache = linkCache;
        this.clock = clock;
    }

    public OwnedLink get(String code, String ownerToken) {
        OwnedLink ownedLink = linkRepository.findOwnedByCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));
        try {
            ownerTokenService.verify(ownerToken, ownedLink.ownerTokenHash());
        } catch (com.example.shortener.link.error.LinkAccessDeniedException exception) {
            log.warn("audit action=link_authorize outcome=denied code={}", code);
            throw exception;
        }
        log.info("audit action=link_authorize outcome=success code={}", code);
        return ownedLink;
    }

    public OwnedLink update(String code, String ownerToken, long expectedVersion,
                            String destinationUrl, LinkStatus status, Instant expiresAt) {
        OwnedLink current = get(code, ownerToken);
        if (destinationUrl == null && status == null && expiresAt == null) {
            throw new InvalidLinkUpdateException("At least one mutable field must be supplied");
        }
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new InvalidExpirationException("Expiration must be in the future");
        }

        Link existing = current.link();
        String updatedDestination = destinationUrl == null
                ? existing.destinationUrl() : destinationUrlPolicy.validate(destinationUrl);
        Link updated = new Link(existing.id(), existing.code(), updatedDestination,
                status == null ? existing.status() : status,
                expiresAt == null ? existing.expiresAt() : expiresAt,
                existing.createdAt(), clock.instant());
        if (!linkRepository.update(new OwnedLink(updated, current.ownerTokenHash(), expectedVersion + 1),
                expectedVersion)) {
            throw new ConcurrentLinkUpdateException(code);
        }
        linkCache.evict(code);
        log.info("audit action=link_update outcome=success code={} version={}", code, expectedVersion + 1);
        return new OwnedLink(updated, current.ownerTokenHash(), expectedVersion + 1);
    }

    public void delete(String code, String ownerToken, long expectedVersion) {
        if (expectedVersion < 0) {
            throw new InvalidLinkUpdateException("Expected version must not be negative");
        }
        OwnedLink current = get(code, ownerToken);
        if (!linkRepository.delete(code, current.ownerTokenHash(), expectedVersion)) {
            throw new ConcurrentLinkUpdateException(code);
        }
        linkCache.evict(code);
        log.info("audit action=link_delete outcome=success code={} version={}", code, expectedVersion);
    }
}
