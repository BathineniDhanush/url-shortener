package com.example.shortener.link.application;

import com.example.shortener.link.domain.DestinationUrlPolicy;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.ShortCodeGenerator;
import com.example.shortener.link.error.CodeGenerationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateLinkServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void retriesGeneratedCodeCollisions() {
        LinkRepository repository = mock(LinkRepository.class);
        ShortCodeGenerator generator = mock(ShortCodeGenerator.class);
        when(generator.generate()).thenReturn("collision1", "collision2", "available1");
        var attempts = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(invocation -> {
            if (attempts.getAndIncrement() < 2) {
                throw new DuplicateKeyException("collision");
            }
            return null;
        }).when(repository).insert(any());

        var service = new CreateLinkService(repository, generator, new DestinationUrlPolicy(), clock,
                new OwnerTokenService());
        CreatedLink created = service.create(new CreateLinkCommand("https://example.com", null, null));

        assertThat(created.link().code()).isEqualTo("available1");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void failsAfterBoundedGeneratedCodeAttempts() {
        LinkRepository repository = mock(LinkRepository.class);
        ShortCodeGenerator generator = mock(ShortCodeGenerator.class);
        when(generator.generate()).thenReturn("collision1");
        doAnswer(invocation -> { throw new DuplicateKeyException("collision"); })
                .when(repository).insert(any());
        var service = new CreateLinkService(repository, generator, new DestinationUrlPolicy(), clock,
                new OwnerTokenService());

        assertThatThrownBy(() -> service.create(new CreateLinkCommand("https://example.com", null, null)))
                .isInstanceOf(CodeGenerationException.class);
    }
}
