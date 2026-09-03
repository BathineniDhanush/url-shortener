package com.example.shortener.link.domain;

import com.example.shortener.link.error.InvalidDestinationUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
            rejectPrivateDestination(uri.getHost());
            return uri.toASCIIString();
        } catch (URISyntaxException exception) {
            throw new InvalidDestinationUrlException("Destination URL is not a valid URI", exception);
        }
    }

    private void rejectPrivateDestination(String rawHost) {
        String host = rawHost.toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")
                || host.endsWith(".internal") || host.equals("metadata.google.internal")) {
            throw new InvalidDestinationUrlException("Destination URL must not target a private network");
        }
        boolean ipLiteral = host.contains(":") || host.matches("[0-9.]+");
        if (!ipLiteral) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            byte[] bytes = address.getAddress();
            boolean carrierGradeNat = bytes.length == 4 && (bytes[0] & 0xff) == 100
                    && ((bytes[1] & 0xff) >= 64 && (bytes[1] & 0xff) <= 127);
            boolean uniqueLocalIpv6 = bytes.length == 16 && ((bytes[0] & 0xfe) == 0xfc);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()
                    || carrierGradeNat || uniqueLocalIpv6) {
                throw new InvalidDestinationUrlException("Destination URL must not target a private network");
            }
        } catch (UnknownHostException exception) {
            throw new InvalidDestinationUrlException("Destination URL contains an invalid IP address", exception);
        }
    }
}
