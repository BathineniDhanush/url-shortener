package com.example.shortener.link.security;

import com.example.shortener.link.application.OwnerTokenService;
import com.example.shortener.link.error.LinkAccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerTokenServiceTest {
    private final OwnerTokenService service = new OwnerTokenService();

    @Test
    void generatesHighEntropyTokensAndVerifiesOnlyTheCorrectValue() {
        String first = service.generate();
        String second = service.generate();
        String hash = service.hash(first);

        assertThat(first).hasSize(43).isNotEqualTo(second);
        assertThat(hash).hasSize(64).doesNotContain(first);
        assertThatCode(() -> service.verify(first, hash)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.verify(second, hash)).isInstanceOf(LinkAccessDeniedException.class);
        assertThatThrownBy(() -> service.verify("", hash)).isInstanceOf(LinkAccessDeniedException.class);
        assertThatThrownBy(() -> service.verify(first, null)).isInstanceOf(LinkAccessDeniedException.class);
    }
}
