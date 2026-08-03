package com.fastaid.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmergencyHelplineRegistryTest {
    @Test
    public void indiaProfileIncludesOfficialCoreAndSpecializedLines() {
        EmergencyHelplineRegistry.Profile profile = EmergencyHelplineRegistry.profileFor("in");

        assertEquals("India", profile.countryName);
        assertEquals("112", profile.primary.number);
        assertEquals(3, profile.core.size());
        assertEquals("100", profile.core.get(0).number);
        assertEquals("101", profile.core.get(1).number);
        assertEquals("108", profile.core.get(2).number);
        assertEquals(3, profile.specialized.size());
        assertEquals("1098", profile.specialized.get(0).number);
        assertEquals("181", profile.specialized.get(1).number);
        assertEquals("1930", profile.specialized.get(2).number);
        assertFalse(profile.fallback);
    }

    @Test
    public void simpleCountryProfilesDoNotShowIndiaSpecificHelplines() {
        EmergencyHelplineRegistry.Profile profile = EmergencyHelplineRegistry.profileFor("US");

        assertEquals("911", profile.primary.number);
        assertTrue(profile.core.isEmpty());
        assertTrue(profile.specialized.isEmpty());
        assertFalse(profile.fallback);
    }

    @Test
    public void unknownCountryUsesExplicitFallbackProfile() {
        EmergencyHelplineRegistry.Profile profile = EmergencyHelplineRegistry.profileFor("XX");

        assertEquals("112", profile.primary.number);
        assertTrue(profile.core.isEmpty());
        assertTrue(profile.specialized.isEmpty());
        assertTrue(profile.fallback);
    }
}
