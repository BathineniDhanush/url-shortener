package com.example.shortener.link.application;

import com.example.shortener.link.domain.DestinationUrlPolicy;
import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.domain.ShortCodeGenerator;
import com.example.shortener.link.domain.OwnedLink;
import com.example.shortener.link.error.AliasConflictException;
import com.example.shortener.link.error.CodeGenerationException;
import com.example.shortener.link.error.InvalidExpirationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateLinkService {
    private static final Logger log = LoggerFactory.getLogger(CreateLinkService.class);
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final Set<String> RESERVED_CODES = Set.of("actuator", "api", "health");

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final DestinationUrlPolicy destinationUrlPolicy;
    private final Clock clock;
    private final OwnerTokenService ownerTokenService;

    public CreateLinkService(LinkRepository linkRepository, ShortCodeGenerator shortCodeGenerator,
                             DestinationUrlPolicy destinationUrlPolicy, Clock clock,
                             OwnerTokenService ownerTokenService) {
        this.linkRepository = linkRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.destinationUrlPolicy = destinationUrlPolicy;
        this.clock = clock;
        this.ownerTokenService = ownerTokenService;
    }

    public CreatedLink create(CreateLinkCommand command) {
        Instant now = clock.instant();
        if (command.expiresAt() != null && !command.expiresAt().isAfter(now)) {
            throw new InvalidExpirationException("Expiration must be in the future");
        }
        String destinationUrl = destinationUrlPolicy.validate(command.destinationUrl());
        if (command.customAlias() != null) {
            if (RESERVED_CODES.contains(command.customAlias().toLowerCase(Locale.ROOT))) {
                throw new AliasConflictException(command.customAlias());
            }
            return insert(command.customAlias(), destinationUrl, command.expiresAt(), now, true);
        }
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            CreatedLink createdLink = insert(shortCodeGenerator.generate(), destinationUrl, command.expiresAt(), now, false);
            if (createdLink != null) {
                return createdLink;
            }
        }
        throw new CodeGenerationException();
    }

    private CreatedLink insert(String code, String destinationUrl, Instant expiresAt, Instant now, boolean customAlias) {
        Link link = new Link(UUID.randomUUID(), code, destinationUrl, LinkStatus.ACTIVE, expiresAt, now, now);
        String ownerToken = ownerTokenService.generate();
        try {
            linkRepository.insert(new OwnedLink(link, ownerTokenService.hash(ownerToken), 0));
            log.info("audit action=link_create outcome=success code={}", code);
            return new CreatedLink(link, ownerToken, 0);
        } catch (DuplicateKeyException exception) {
            if (customAlias) {
                throw new AliasConflictException(code);
            }
            return null;
        }
    }
}
