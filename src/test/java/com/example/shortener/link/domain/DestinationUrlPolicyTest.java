package com.example.shortener.link.domain;

import com.example.shortener.link.error.InvalidDestinationUrlException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DestinationUrlPolicyTest {
    private final DestinationUrlPolicy policy = new DestinationUrlPolicy();

    @Test
    void acceptsHttpAndHttpsUrls() {
        assertThat(policy.validate("https://example.com/path?q=1#part"))
                .isEqualTo("https://example.com/path?q=1#part");
        assertThat(policy.validate("http://example.com"))
                .isEqualTo("http://example.com");
    }

    @Test
    void rejectsUnsafeSchemesAndEmbeddedCredentials() {
        assertThatThrownBy(() -> policy.validate("javascript:alert(1)"))
                .isInstanceOf(InvalidDestinationUrlException.class);
        assertThatThrownBy(() -> policy.validate("https://user:secret@example.com"))
                .isInstanceOf(InvalidDestinationUrlException.class);
    }

    @Test
    void rejectsUrlsWithoutAValidHost() {
        assertThatThrownBy(() -> policy.validate("https:///missing-host"))
                .isInstanceOf(InvalidDestinationUrlException.class);
    }
}
