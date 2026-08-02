package com.fastaid.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class EmergencyNumberResolverTest {
    @Test
    public void resolvesKnownCountryNumbers() {
        assertEquals("112", EmergencyNumberResolver.resolve("IN"));
        assertEquals("911", EmergencyNumberResolver.resolve("us"));
        assertEquals("999", EmergencyNumberResolver.resolve("GB"));
        assertEquals("000", EmergencyNumberResolver.resolve("AU"));
    }

    @Test
    public void leavesUnknownCountryForExplicitFallback() {
        assertNull(EmergencyNumberResolver.resolve(null));
        assertNull(EmergencyNumberResolver.resolve("XX"));
    }
}
