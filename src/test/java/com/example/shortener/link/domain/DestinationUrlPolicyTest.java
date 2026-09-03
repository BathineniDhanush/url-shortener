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

    @Test
    void rejectsLocalPrivateAndMetadataDestinations() {
        for (String candidate : new String[]{
                "http://localhost/admin", "http://127.0.0.1", "http://10.1.2.3",
                "http://169.254.169.254/latest/meta-data", "http://[::1]",
                "https://service.internal/path", "https://metadata.google.internal/computeMetadata/v1"}) {
            assertThatThrownBy(() -> policy.validate(candidate))
                    .as(candidate)
                    .isInstanceOf(InvalidDestinationUrlException.class)
                    .hasMessageContaining("private network");
        }
    }
}
