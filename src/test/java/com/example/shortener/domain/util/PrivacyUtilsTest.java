package com.example.shortener.domain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void anonymizesValidIpv4ToTwentyFourBitPrefix() {
        assertEquals("192.0.2.0", PrivacyUtils.anonymizeIp("192.0.2.187"));
        assertEquals("0.0.0.0", PrivacyUtils.anonymizeIp("0.0.0.0"));
        assertEquals("255.255.255.0", PrivacyUtils.anonymizeIp("255.255.255.255"));
    }

    @Test
    void anonymizesExpandedAndCompressedIpv6ToSixtyFourBitPrefix() {
        assertEquals("2001:db8:85a3:0::",
            PrivacyUtils.anonymizeIp("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertEquals("2001:db8:0:0::", PrivacyUtils.anonymizeIp("2001:db8::1"));
    }

    @Test
    void rejectsMalformedIpAddressesAndHandlesMissingValues() {
        assertEquals("unknown", PrivacyUtils.anonymizeIp("999.2.3.4"));
        assertEquals("unknown", PrivacyUtils.anonymizeIp("not-an-ip"));
        assertEquals("unknown", PrivacyUtils.anonymizeIp("2001:::1"));
        assertNull(PrivacyUtils.anonymizeIp(null));
        assertNull(PrivacyUtils.anonymizeIp("   "));
    }
}
