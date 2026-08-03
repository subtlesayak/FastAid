package com.fastaid.app;

import java.util.Locale;

final class EmergencyNumberResolver {
    private EmergencyNumberResolver() {
    }

    static String resolve(String countryCode) {
        if (countryCode == null) return null;
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        if (!EmergencyHelplineRegistry.hasCuratedProfile(normalized)) return null;
        return EmergencyHelplineRegistry.primaryNumber(normalized);
    }
}
