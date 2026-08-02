package com.fastaid.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class AidPlaceCache {
    static final class Snapshot {
        final List<AidPlace> places;
        final long ageMillis;

        Snapshot(List<AidPlace> places, long ageMillis) {
            this.places = places;
            this.ageMillis = ageMillis;
        }

        boolean isStale() {
            return ageMillis > 6L * 60L * 60L * 1000L;
        }
    }

    private static final String PREFS = "fastaid_places_cache";
    private static final String KEY_RESULTS = "results";
    private static final String KEY_SAVED_AT = "saved_at";
    private static final long MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L;

    private final SharedPreferences preferences;

    AidPlaceCache(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void clear() {
        preferences.edit().clear().apply();
    }

    void save(List<AidPlace> places) {
        JSONArray array = new JSONArray();
        for (AidPlace place : places) {
            if (place.provider == null || !place.provider.startsWith("google_places")) {
                continue;
            }
            JSONObject item = new JSONObject();
            try {
                item.put("id", place.id);
                item.put("name", place.name);
                item.put("category", place.category);
                item.put("distance", place.distance);
                item.put("eta", place.eta);
                item.put("phone", place.phone);
                item.put("address", place.address);
                item.put("provider", place.provider);
                item.put("openText", place.openText);
                item.put("lat", place.latitude);
                item.put("lng", place.longitude);
                item.put("verified", place.verified);
                item.put("openKnown", place.openKnown);
                item.put("openNow", place.openNow);
                array.put(item);
            } catch (Exception ignored) {
                // Skip malformed entries; never cache invented replacements.
            }
        }
        if (array.length() == 0) {
            return;
        }
        preferences.edit()
                .putString(KEY_RESULTS, array.toString())
                .putLong(KEY_SAVED_AT, System.currentTimeMillis())
                .apply();
    }

    Snapshot load() {
        long savedAt = preferences.getLong(KEY_SAVED_AT, 0L);
        long age = savedAt == 0L ? Long.MAX_VALUE : Math.max(0L, System.currentTimeMillis() - savedAt);
        if (age > MAX_AGE_MILLIS) {
            return new Snapshot(new ArrayList<>(), age);
        }
        List<AidPlace> places = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(KEY_RESULTS, "[]"));
            for (int index = 0; index < array.length(); index++) {
                JSONObject item = array.optJSONObject(index);
                if (item == null) continue;
                places.add(new AidPlace(
                        item.optString("id", ""),
                        item.optString("name", "Nearby aid"),
                        item.optString("category", "aid"),
                        item.optString("distance", "Distance unavailable"),
                        item.optString("eta", "ETA unavailable"),
                        item.optString("phone", ""),
                        item.optString("address", ""),
                        item.optString("provider", "google_places_cache"),
                        "Hours need a live refresh",
                        item.optDouble("lat", Double.NaN),
                        item.optDouble("lng", Double.NaN),
                        item.optBoolean("verified", false),
                        item.optBoolean("openKnown", false),
                        item.optBoolean("openNow", false)
                ));
            }
        } catch (Exception ignored) {
            places.clear();
        }
        return new Snapshot(places, age);
    }
}