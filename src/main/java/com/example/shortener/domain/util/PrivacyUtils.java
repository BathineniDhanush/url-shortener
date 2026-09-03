package com.example.shortener.domain.util;

import java.util.Optional;
import java.util.regex.Pattern;

public class PrivacyUtils {

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\.\\d{1,3}$");
    private static final Pattern IPV6_PATTERN = Pattern.compile("^([a-fA-F0-9:]+:[a-fA-F0-9:]+:[a-fA-F0-9:]+:[a-fA-F0-9:]+):[a-fA-F0-9:]+$");

    public static String anonymizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }

        var v4Matcher = IPV4_PATTERN.matcher(ip);
        if (v4Matcher.matches()) {
            return v4Matcher.group(1) + ".0";
        }

        var v6Matcher = IPV6_PATTERN.matcher(ip);
        if (v6Matcher.matches()) {
            return v6Matcher.group(1) + "::0";
        }

        return "unknown";
    }

    public static String filterUserAgent(String ua) {
        if (ua == null || ua.isBlank()) {
            return "unknown";
        }
        // Basic sanitization: trim and limit length to prevent log injection/bloat
        return ua.trim();
    }
}
