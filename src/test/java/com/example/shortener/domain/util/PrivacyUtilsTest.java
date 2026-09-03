package com.example.shortener.domain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PrivacyUtilsTest {

    @Test
    void stripsHeaderControlCharactersAndLimitsUserAgentLength() {
        String filtered = PrivacyUtils.filterUserAgent("browser\r\n" + "x".repeat(600));

        assertEquals(512, filtered.length());
        assertFalse(filtered.contains("\r"));
        assertFalse(filtered.contains("\n"));
        assertEquals("browser  ", filtered.substring(0, 9));
    }

    @Test
    void substitutesUnknownForMissingUserAgent() {
        assertEquals("unknown", PrivacyUtils.filterUserAgent(null));
        assertEquals("unknown", PrivacyUtils.filterUserAgent("   "));
    }
}
