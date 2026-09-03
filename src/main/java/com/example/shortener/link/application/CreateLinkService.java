package com.example.shortener.link.application;

import com.example.shortener.link.domain.DestinationUrlPolicy;
import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.domain.ShortCodeGenerator;
import com.example.shortener.link.error.AliasConflictException;
import com.example.shortener.link.error.CodeGenerationException;
import com.example.shortener.link.error.InvalidExpirationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class CreateLinkService {
    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final Set<String> RESERVED_CODES = Set.of("actuator", "api", "health");

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final DestinationUrlPolicy destinationUrlPolicy;
    private final Clock clock;

    public CreateLinkService(LinkRepository linkRepository, ShortCodeGenerator shortCodeGenerator,
                             DestinationUrlPolicy destinationUrlPolicy, Clock clock) {
        this.linkRepository = linkRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.destinationUrlPolicy = destinationUrlPolicy;
        this.clock = clock;
    }

    public Link create(CreateLinkCommand command) {
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
            Link link = insert(shortCodeGenerator.generate(), destinationUrl, command.expiresAt(), now, false);
            if (link != null) {
                return link;
            }
        }
        throw new CodeGenerationException();
    }

    private Link insert(String code, String destinationUrl, Instant expiresAt, Instant now, boolean customAlias) {
        Link link = new Link(UUID.randomUUID(), code, destinationUrl, LinkStatus.ACTIVE, expiresAt, now, now);
        try {
            linkRepository.insert(link);
            return link;
        } catch (DuplicateKeyException exception) {
            if (customAlias) {
                throw new AliasConflictException(code);
            }
            return null;
        }
    }
}
