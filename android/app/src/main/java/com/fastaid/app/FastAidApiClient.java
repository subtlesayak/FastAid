package com.fastaid.app;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class FastAidApiClient {
    private final String baseUrl;

    FastAidApiClient(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    boolean isConfigured() {
        return baseUrl.startsWith("https://") || baseUrl.startsWith("http://");
    }

    List<AidPlace> fetchNearbyAid(double latitude, double longitude, String incidentType) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("FastAid backend is not configured");
        }
        Uri uri = Uri.parse(baseUrl + "/api/nearby/aid")
                .buildUpon()
                .appendQueryParameter("lat", String.valueOf(latitude))
                .appendQueryParameter("lng", String.valueOf(longitude))
                .appendQueryParameter("incidentType", incidentType)
                .build();

        JSONObject payload = requestJson("GET", uri.toString(), null);
        String payloadProvider = payload.optString("provider", "");
        if ("mock".equalsIgnoreCase(payloadProvider)) {
            throw new IllegalStateException("Backend returned mock nearby places");
        }
        JSONArray places = payload.optJSONArray("results");
        List<AidPlace> results = new ArrayList<>();
        if (places == null) {
            return results;
        }

        for (int index = 0; index < places.length(); index++) {
            JSONObject item = places.optJSONObject(index);
            if (item != null) {
                results.add(parseAidPlace(item));
            }
        }
        return results;
    }

    String createIncident(double latitude, double longitude, String incidentType, int patientCount, String notes) throws Exception {
        JSONObject body = new JSONObject();
        body.put("type", incidentType);
        body.put("peopleCount", patientCount);
        body.put("note", notes == null ? "" : notes);
        body.put("locationText", String.format(Locale.US, "GPS %.5f, %.5f", latitude, longitude));
        body.put("location", new JSONObject()
                .put("lat", latitude)
                .put("lng", longitude)
                .put("label", "Android incident"));

        JSONObject payload = requestJson("POST", baseUrl + "/api/incidents", body);
        JSONObject incident = payload.optJSONObject("incident");
        return incident == null ? "created" : incident.optString("status", "created");
    }

    private AidPlace parseAidPlace(JSONObject item) {
        JSONObject location = item.optJSONObject("location");
        double latitude = location == null ? Double.NaN : location.optDouble("lat", Double.NaN);
        double longitude = location == null ? Double.NaN : location.optDouble("lng", Double.NaN);
        boolean openKnown = item.optBoolean("openKnown", item.has("openNow"));
        boolean openNow = item.optBoolean("openNow", false);
        String openText = item.optString("open", openKnown ? (openNow ? "Open now" : "Closed now") : "Open status unknown");
        String provider = item.optString("provider", item.optString("source", "public_place"));
        if ("mock".equalsIgnoreCase(provider) || provider.toLowerCase(Locale.ROOT).contains("fallback")) {
            throw new IllegalStateException("Backend returned non-live nearby places");
        }

        return new AidPlace(
                item.optString("id", item.optString("placeId", item.optString("name", "aid"))),
                item.optString("name", "Nearby aid"),
                item.optString("category", item.optString("type", "aid")),
                item.optString("distance", "Distance unavailable"),
                item.optString("eta", "ETA unavailable"),
                item.optString("phone", ""),
                item.optString("address", ""),
                provider,
                openText,
                latitude,
                longitude,
                item.optBoolean("verified", false),
                openKnown,
                openKnown && openNow
        );
    }

    private JSONObject requestJson(String method, String targetUrl, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(targetUrl).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");

        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream stream = connection.getOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
                writer.write(body.toString());
            }
        }

        int statusCode = connection.getResponseCode();
        InputStream stream = statusCode >= 200 && statusCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readAll(stream);
        if (statusCode < 200 || statusCode >= 300) {
            throw new IllegalStateException("FastAid API returned " + statusCode + ": " + response);
        }
        return new JSONObject(response);
    }

    private String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}

