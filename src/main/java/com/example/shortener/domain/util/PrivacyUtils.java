package com.example.shortener.domain.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class PrivacyUtils {
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private PrivacyUtils() {
    }

    public static String anonymizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }

        String candidate = ip.trim();
        if (candidate.indexOf(':') < 0) {
            return anonymizeIpv4(candidate);
        }
        if (candidate.indexOf('%') >= 0) {
            return "unknown";
        }

        return anonymizeIpv6(candidate);
    }

    public static String filterUserAgent(String ua) {
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        String sanitized = ua.replace('\r', ' ').replace('\n', ' ').trim();
        return sanitized.length() <= MAX_USER_AGENT_LENGTH
            ? sanitized
            : sanitized.substring(0, MAX_USER_AGENT_LENGTH);
    }

    private static String anonymizeIpv4(String candidate) {
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) {
            return "unknown";
        }
        for (String octet : octets) {
            if (octet.isEmpty() || !octet.chars().allMatch(Character::isDigit)) {
                return "unknown";
            }
            try {
                if (Integer.parseInt(octet) > 255) {
                    return "unknown";
                }
            } catch (NumberFormatException exception) {
                return "unknown";
            }
        }
        return String.join(".", octets[0], octets[1], octets[2], "0");
    }

    private static String anonymizeIpv6(String candidate) {
        try {
            InetAddress address = InetAddress.getByName(candidate);
            if (!(address instanceof Inet6Address)) {
                return "unknown";
            }
            byte[] bytes = address.getAddress();
            return String.format("%x:%x:%x:%x::",
                unsignedHextet(bytes, 0), unsignedHextet(bytes, 2),
                unsignedHextet(bytes, 4), unsignedHextet(bytes, 6));
        } catch (UnknownHostException exception) {
            return "unknown";
        }
    }

    private static int unsignedHextet(byte[] bytes, int offset) {
        return (Byte.toUnsignedInt(bytes[offset]) << 8) | Byte.toUnsignedInt(bytes[offset + 1]);
    }
}
