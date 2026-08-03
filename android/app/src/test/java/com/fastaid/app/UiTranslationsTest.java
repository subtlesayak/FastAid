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
            assertNotEquals(language, "NGO", UiTranslations.translate(language, "NGO"));
        }
    }

    @Test
    public void regionalTranslationsAreReadableUnicodeNotMojibake() {
        assertEquals("मानचित्र", UiTranslations.translate("Hindi", "Map"));
        assertEquals("মানচিত্র", UiTranslations.translate("Bengali", "Map"));
        assertEquals("മാപ്പ്", UiTranslations.translate("Malayalam", "Map"));
        assertFalse(UiTranslations.translate("Tamil", "Call emergency").contains("à"));
        assertFalse(UiTranslations.translate("Urdu", "Nearby").contains("Ù"));
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

    @Test
    public void indiaShowsSupportedRegionalLanguagesThroughMalayalam() {
        String[] languages = UiTranslations.languagesForCountry("IN");

        assertEquals("English", languages[0]);
        assertTrue(UiTranslations.isLanguageAllowedForCountry("Hindi", "IN"));
        assertTrue(UiTranslations.isLanguageAllowedForCountry("Tamil", "in"));
        assertTrue(UiTranslations.isLanguageAllowedForCountry("Malayalam", "IN"));
    }

    @Test
    public void nonIndiaCountriesOnlyShowEnglishUntilCountryPacksExist() {
        String[] languages = UiTranslations.languagesForCountry("US");

        assertEquals(1, languages.length);
        assertEquals("English", languages[0]);
        assertTrue(UiTranslations.isLanguageAllowedForCountry("English", "US"));
        assertFalse(UiTranslations.isLanguageAllowedForCountry("Hindi", "US"));
        assertFalse(UiTranslations.isLanguageAllowedForCountry("Malayalam", "AE"));
    }
}
