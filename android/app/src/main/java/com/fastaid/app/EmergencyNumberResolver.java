package com.fastaid.app;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class EmergencyNumberResolver {
    private static final Map<String, String> NUMBERS;

    static {
        Map<String, String> values = new HashMap<>();
        values.put("IN", "112");
        values.put("US", "911");
        values.put("CA", "911");
        values.put("GB", "999");
        values.put("AU", "000");
        values.put("NZ", "111");
        values.put("JP", "119");
        values.put("KR", "119");
        values.put("SG", "995");
        values.put("MY", "999");
        values.put("AE", "999");
        values.put("ZA", "112");
        values.put("BR", "192");
        values.put("MX", "911");
        values.put("AR", "911");
        values.put("PH", "911");
        values.put("ID", "112");
        values.put("TH", "1669");
        values.put("VN", "115");
        values.put("LK", "1990");
        values.put("NP", "102");

        String[] european112 = {
                "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE",
                "GR", "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT",
                "RO", "SK", "SI", "ES", "SE", "IS", "LI", "NO", "CH"
        };
        for (String country : european112) values.put(country, "112");
        NUMBERS = Collections.unmodifiableMap(values);
    }

    private EmergencyNumberResolver() {
    }

    static String resolve(String countryCode) {
        if (countryCode == null) return null;
        return NUMBERS.get(countryCode.trim().toUpperCase(Locale.ROOT));
    }
}
