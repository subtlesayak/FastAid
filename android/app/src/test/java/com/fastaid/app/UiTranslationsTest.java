package com.fastaid.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UiTranslationsTest {
    @Test
    public void everySupportedIndianLanguageTranslatesCriticalNavigation() {
        String[] languages = {
                "Hindi", "Bengali", "Telugu", "Marathi", "Tamil", "Gujarati",
                "Urdu", "Kannada", "Odia", "Malayalam"
        };

        for (String language : languages) {
            assertNotEquals(language, "Map", UiTranslations.translate(language, "Map"));
            assertNotEquals(language, "Nearby Aid",
                    UiTranslations.translate(language, "Nearby Aid"));
            assertNotEquals(language, "Call emergency",
                    UiTranslations.translate(language, "Call emergency"));
        }
    }

    @Test
    public void localeTagsAndRtlDirectionMatchSupportedLanguages() {
        assertEquals("hi-IN", UiTranslations.localeTag("Hindi"));
        assertEquals("ml-IN", UiTranslations.localeTag("Malayalam"));
        assertEquals("ur-IN", UiTranslations.localeTag("Urdu"));
        assertTrue(UiTranslations.isRtl("Urdu"));
        assertFalse(UiTranslations.isRtl("Hindi"));
    }

    @Test
    public void unknownLanguageFallsBackToEnglish() {
        assertEquals("Nearby", UiTranslations.translate("Unknown", "Nearby"));
    }
}
