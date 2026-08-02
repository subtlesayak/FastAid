package com.fastaid.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmergencyLocationPolicyTest {
    @Test
    public void sosRequiresValidFreshCoordinates() {
        long now = 1_000_000L;
        assertTrue(EmergencyLocationPolicy.canSendSos(12.9, 77.6, now - 1_000L, now));
        assertFalse(EmergencyLocationPolicy.canSendSos(Double.NaN, 77.6, now, now));
        assertFalse(EmergencyLocationPolicy.canSendSos(12.9, 77.6,
                now - EmergencyLocationPolicy.SOS_MAX_AGE_MILLIS - 1L, now));
    }

    @Test
    public void toleratesSmallGpsClockSkewButRejectsFutureFixes() {
        long now = 1_000_000L;
        assertTrue(EmergencyLocationPolicy.canSearchNearby(
                12.9, 77.6, now + 1_000L, now));
        assertFalse(EmergencyLocationPolicy.canSearchNearby(
                12.9, 77.6, now + EmergencyLocationPolicy.MAX_FUTURE_SKEW_MILLIS + 1L, now));
    }

    @Test
    public void newerOrMoreAccurateFixWins() {
        assertTrue(EmergencyLocationPolicy.isBetterFix(200_000L, 80f, 1_000L, 10f));
        assertTrue(EmergencyLocationPolicy.isBetterFix(100_000L, 5f, 100_000L, 20f));
        assertFalse(EmergencyLocationPolicy.isBetterFix(100_000L, 50f, 100_000L, 20f));
    }
}
