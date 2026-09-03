package com.example.shortener.link.domain;

import com.example.shortener.link.error.InvalidDestinationUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

@Component
public class DestinationUrlPolicy {
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public String validate(String candidate) {
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme();
            if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
                throw new InvalidDestinationUrlException("Destination URL must use HTTP or HTTPS");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidDestinationUrlException("Destination URL must include a valid host");
            }
            if (uri.getUserInfo() != null) {
                throw new InvalidDestinationUrlException("Destination URL must not contain credentials");
            }
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new InvalidDestinationUrlException("Destination URL is not a valid URI", exception);
        }
    }
}
