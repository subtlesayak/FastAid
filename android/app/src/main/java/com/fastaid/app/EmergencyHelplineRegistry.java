package com.fastaid.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EmergencyHelplineRegistry {
    static final class Entry {
        final String number;
        final String label;
        final String description;
        final String kind;

        Entry(String number, String label, String description, String kind) {
            this.number = number;
            this.label = label;
            this.description = description;
            this.kind = kind;
        }
    }

    static final class Profile {
        final String countryCode;
        final String countryName;
        final Entry primary;
        final List<Entry> core;
        final List<Entry> specialized;
        final boolean fallback;

        Profile(
                String countryCode,
                String countryName,
                Entry primary,
                List<Entry> core,
                List<Entry> specialized,
                boolean fallback
        ) {
            this.countryCode = countryCode;
            this.countryName = countryName;
            this.primary = primary;
            this.core = Collections.unmodifiableList(new ArrayList<>(core));
            this.specialized = Collections.unmodifiableList(new ArrayList<>(specialized));
            this.fallback = fallback;
        }
    }

    private static final Map<String, Profile> PROFILES;

    static {
        Map<String, Profile> profiles = new HashMap<>();

        put(profiles, profile("IN", "India",
                entry("112", "National emergency", "Unified police, fire, and health response", "unified"),
                list(
                        entry("100", "Police", "Direct police control room", "police"),
                        entry("101", "Fire", "Fire and rescue services", "fire"),
                        entry("108", "EMS", "Emergency ambulance service", "medical")
                ),
                list(
                        entry("1098", "Child", "Child in distress helpline", "child"),
                        entry("181", "Women", "Women's helpline", "women"),
                        entry("1930", "Cyber", "Cyber crime reporting helpline", "cyber")
                )));

        put(profiles, simple("US", "United States", "911"));
        put(profiles, simple("CA", "Canada", "911"));
        put(profiles, simple("MX", "Mexico", "911"));
        put(profiles, simple("AR", "Argentina", "911"));
        put(profiles, simple("PH", "Philippines", "911"));

        put(profiles, profile("GB", "United Kingdom",
                entry("999", "Emergency", "Police, fire, ambulance, and coastguard", "unified"),
                list(entry("112", "Emergency", "Alternative emergency access", "unified")),
                Collections.emptyList()));

        put(profiles, profile("AU", "Australia",
                entry("000", "Emergency", "Police, fire, and ambulance", "unified"),
                list(
                        entry("112", "Mobile SOS", "Emergency access from mobile phones", "unified"),
                        entry("106", "TTY emergency", "Text-based emergency relay", "accessibility")
                ),
                Collections.emptyList()));

        put(profiles, simple("NZ", "New Zealand", "111"));

        put(profiles, profile("JP", "Japan",
                entry("119", "Fire / ambulance", "Fire and emergency medical services", "medical"),
                list(entry("110", "Police", "Police emergency", "police")),
                Collections.emptyList()));

        put(profiles, profile("KR", "South Korea",
                entry("119", "Fire / ambulance", "Fire and emergency medical services", "medical"),
                list(entry("112", "Police", "Police emergency", "police")),
                Collections.emptyList()));

        put(profiles, profile("SG", "Singapore",
                entry("995", "Fire / ambulance", "Emergency ambulance and fire services", "medical"),
                list(entry("999", "Police", "Police emergency", "police")),
                Collections.emptyList()));

        put(profiles, simple("MY", "Malaysia", "999"));

        put(profiles, profile("AE", "United Arab Emirates",
                entry("999", "Police", "Police emergency", "police"),
                list(
                        entry("998", "Ambulance", "Emergency ambulance", "medical"),
                        entry("997", "Fire", "Fire emergency", "fire")
                ),
                Collections.emptyList()));

        put(profiles, profile("ZA", "South Africa",
                entry("112", "Mobile emergency", "Emergency access from mobile phones", "unified"),
                list(
                        entry("10111", "Police", "Police emergency", "police"),
                        entry("10177", "Ambulance", "Emergency ambulance", "medical")
                ),
                Collections.emptyList()));

        put(profiles, profile("BR", "Brazil",
                entry("192", "Ambulance", "SAMU emergency ambulance", "medical"),
                list(
                        entry("190", "Police", "Police emergency", "police"),
                        entry("193", "Fire", "Fire emergency", "fire")
                ),
                Collections.emptyList()));

        put(profiles, profile("ID", "Indonesia",
                entry("112", "Emergency", "Integrated emergency response", "unified"),
                list(
                        entry("110", "Police", "Police emergency", "police"),
                        entry("113", "Fire", "Fire emergency", "fire"),
                        entry("119", "Ambulance", "Emergency ambulance", "medical")
                ),
                Collections.emptyList()));

        put(profiles, profile("TH", "Thailand",
                entry("1669", "Medical", "Emergency medical services", "medical"),
                list(
                        entry("191", "Police", "Police emergency", "police"),
                        entry("199", "Fire", "Fire emergency", "fire")
                ),
                Collections.emptyList()));

        put(profiles, profile("VN", "Vietnam",
                entry("115", "Ambulance", "Emergency ambulance", "medical"),
                list(
                        entry("113", "Police", "Police emergency", "police"),
                        entry("114", "Fire", "Fire emergency", "fire")
                ),
                Collections.emptyList()));

        put(profiles, profile("LK", "Sri Lanka",
                entry("1990", "Ambulance", "Emergency ambulance", "medical"),
                list(entry("119", "Police", "Police emergency", "police")),
                Collections.emptyList()));

        put(profiles, profile("NP", "Nepal",
                entry("102", "Ambulance", "Emergency ambulance", "medical"),
                list(
                        entry("100", "Police", "Police emergency", "police"),
                        entry("101", "Fire", "Fire emergency", "fire")
                ),
                Collections.emptyList()));

        String[] european112 = {
                "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
                "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT",
                "RO", "SK", "SI", "ES", "SE", "IS", "LI", "NO", "CH"
        };
        for (String country : european112) {
            put(profiles, simple(country, countryName(country), "112"));
        }

        PROFILES = Collections.unmodifiableMap(profiles);
    }

    private EmergencyHelplineRegistry() {
    }

    static Profile profileFor(String countryCode) {
        String normalized = normalize(countryCode);
        Profile profile = PROFILES.get(normalized);
        if (profile != null) return profile;
        return new Profile(
                normalized,
                normalized.isEmpty() ? "Current country" : countryName(normalized),
                entry("112", "Emergency", "Fallback emergency number; verify locally", "unified"),
                Collections.emptyList(),
                Collections.emptyList(),
                true);
    }

    static String primaryNumber(String countryCode) {
        return profileFor(countryCode).primary.number;
    }

    static boolean hasCuratedProfile(String countryCode) {
        return PROFILES.containsKey(normalize(countryCode));
    }

    private static void put(Map<String, Profile> profiles, Profile profile) {
        profiles.put(profile.countryCode, profile);
    }

    private static Profile simple(String countryCode, String countryName, String number) {
        return profile(countryCode, countryName,
                entry(number, "Emergency", "Police, fire, and medical emergency response", "unified"),
                Collections.emptyList(),
                Collections.emptyList());
    }

    private static Profile profile(
            String countryCode,
            String countryName,
            Entry primary,
            List<Entry> core,
            List<Entry> specialized
    ) {
        return new Profile(normalize(countryCode), countryName, primary, core, specialized, false);
    }

    private static Entry entry(String number, String label, String description, String kind) {
        return new Entry(number, label, description, kind);
    }

    private static List<Entry> list(Entry... entries) {
        return Arrays.asList(entries);
    }

    private static String normalize(String countryCode) {
        return countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private static String countryName(String countryCode) {
        String normalized = normalize(countryCode);
        if (normalized.isEmpty()) return "Current country";
        String display = new Locale("", normalized).getDisplayCountry(Locale.ENGLISH);
        return display == null || display.trim().isEmpty() ? normalized : display;
    }
}
