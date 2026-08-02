package com.fastaid.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AidPlaceTest {
    @Test
    public void phoneAvailabilityRequiresNonBlankNumber() {
        assertFalse(place(" ", 28.6, 77.2).hasPhone());
        assertTrue(place("112", 28.6, 77.2).hasPhone());
    }

    @Test
    public void coordinatesRejectMissingMapData() {
        assertFalse(place("112", Double.NaN, Double.NaN).hasCoordinates());
        assertTrue(place("112", 28.6, 77.2).hasCoordinates());
    }

    private AidPlace place(String phone, double latitude, double longitude) {
        return new AidPlace(
                "test-place",
                "Test Aid",
                "hospital",
                "1 km",
                "4 min",
                phone,
                "Test address",
                "google_places",
                "Open now",
                latitude,
                longitude,
                false,
                true,
                true
        );
    }
}
