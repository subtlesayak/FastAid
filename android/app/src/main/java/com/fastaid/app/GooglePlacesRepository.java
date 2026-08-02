package com.fastaid.app;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.IsOpenRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.libraries.places.api.net.SearchNearbyResponse;
import com.google.android.libraries.places.api.Places;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class GooglePlacesRepository {
    interface Callback {
        void onSuccess(List<AidPlace> places);
        void onFailure(Exception error);
    }

    private static final double DEFAULT_SEARCH_RADIUS_METERS = 5000.0;
    private static final double EMERGENCY_SEARCH_RADIUS_METERS = 15000.0;
    private static final int MAX_RESULTS = 20;

    private final PlacesClient client;

    GooglePlacesRepository(Context context, String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("A Places API key is required");
        }
        Context appContext = context.getApplicationContext();
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(appContext, apiKey.trim());
        }
        client = Places.createClient(appContext);
    }

    void search(double latitude, double longitude, String incidentType, Callback callback) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.DISPLAY_NAME,
                Place.Field.LOCATION,
                Place.Field.FORMATTED_ADDRESS,
                Place.Field.NATIONAL_PHONE_NUMBER,
                Place.Field.BUSINESS_STATUS,
                Place.Field.CURRENT_OPENING_HOURS,
                Place.Field.UTC_OFFSET,
                Place.Field.PRIMARY_TYPE,
                Place.Field.TYPES
        );
        CircularBounds bounds = CircularBounds.newInstance(
                new LatLng(latitude, longitude), searchRadiusMeters(incidentType));
        SearchNearbyRequest request = SearchNearbyRequest.builder(bounds, fields)
                .setIncludedTypes(includedTypes(incidentType))
                .setMaxResultCount(MAX_RESULTS)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                .build();

        client.searchNearby(request)
                .addOnSuccessListener(response -> resolveOpenStatus(
                        response, latitude, longitude, incidentType, callback))
                .addOnFailureListener(error -> callback.onFailure(asException(error)));
    }

    private void resolveOpenStatus(
            SearchNearbyResponse response,
            double originLatitude,
            double originLongitude,
            String incidentType,
            Callback callback
    ) {
        List<Place> googlePlaces = response.getPlaces();
        if (googlePlaces == null || googlePlaces.isEmpty()) {
            callback.onSuccess(Collections.emptyList());
            return;
        }

        List<AidPlace> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger remaining = new AtomicInteger(googlePlaces.size());
        AtomicBoolean completed = new AtomicBoolean(false);

        for (Place place : googlePlaces) {
            client.isOpen(IsOpenRequest.newInstance(place)).addOnCompleteListener(task -> {
                Boolean openNow = null;
                if (task.isSuccessful() && task.getResult() != null) {
                    openNow = task.getResult().isOpen();
                }
                AidPlace aidPlace = toAidPlace(place, originLatitude, originLongitude, openNow);
                if (aidPlace != null) {
                    results.add(aidPlace);
                }
                if (remaining.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
                    List<AidPlace> sorted = new ArrayList<>(results);
                    sortPlaces(sorted, incidentType);
                    callback.onSuccess(sorted);
                }
            });
        }
    }

    private AidPlace toAidPlace(
            Place place,
            double originLatitude,
            double originLongitude,
            Boolean openNow
    ) {
        LatLng location = place.getLocation();
        if (location == null || place.getId() == null || place.getDisplayName() == null) {
            return null;
        }
        double distanceKm = distanceKm(originLatitude, originLongitude,
                location.latitude, location.longitude);
        boolean openKnown = openNow != null;
        String openText = openKnown ? (Boolean.TRUE.equals(openNow) ? "Open now" : "Closed now")
                : "Hours unavailable";
        return new AidPlace(
                place.getId(),
                place.getDisplayName(),
                normalizeCategory(place.getPrimaryType(), placeTypeNames(place)),
                formatDistance(distanceKm),
                estimateEta(distanceKm),
                safe(place.getNationalPhoneNumber()),
                safe(place.getFormattedAddress()),
                "google_places_sdk",
                openText,
                location.latitude,
                location.longitude,
                false,
                openKnown,
                Boolean.TRUE.equals(openNow)
        );
    }

    static void sortPlaces(List<AidPlace> places) {
        sortPlaces(places, "accident");
    }

    static void sortPlaces(List<AidPlace> places, String incidentType) {
        places.sort((left, right) -> {
            int leftRelevance = ServiceQualityScanner.relevanceRank(incidentType, left);
            int rightRelevance = ServiceQualityScanner.relevanceRank(incidentType, right);
            int relevance = Integer.compare(leftRelevance, rightRelevance);
            if (relevance != 0) return relevance;
            int availability = Integer.compare(availabilityRank(left), availabilityRank(right));
            if (availability != 0) return availability;
            int quality = Integer.compare(
                    ServiceQualityScanner.score(right),
                    ServiceQualityScanner.score(left));
            if (quality != 0) return quality;
            if (left.hasPhone() != right.hasPhone()) return left.hasPhone() ? -1 : 1;
            int distance = Double.compare(numericDistance(left.distance), numericDistance(right.distance));
            if (distance != 0) return distance;
            return left.name.compareToIgnoreCase(right.name);
        });
    }

    private static int availabilityRank(AidPlace place) {
        if (place.openKnown && place.openNow) return 0;
        if (!place.openKnown) return 1;
        return 2;
    }

    static List<String> includedTypes(String incidentType) {
        String type = incidentType == null ? "accident" : incidentType.toLowerCase(Locale.US);
        if (type.contains("accident") || type.contains("emergency")) {
            return Arrays.asList("hospital", "general_hospital", "medical_center", "police", "fire_station");
        }
        if (type.contains("medical_lab") || type.equals("lab")) {
            return Collections.singletonList("medical_lab");
        }
        if (type.contains("clinic")) {
            return Arrays.asList("medical_clinic", "medical_center", "doctor");
        }
        if (type.contains("pharmacy") || type.contains("drugstore")) {
            return Arrays.asList("pharmacy", "drugstore");
        }
        if (type.contains("toilet") || type.contains("bathroom")) {
            return Arrays.asList("public_bathroom", "public_bath");
        }
        if (type.contains("tyre") || type.contains("tire")) {
            return Collections.singletonList("tire_shop");
        }
        if (type.contains("rest_stop") || type.contains("rest stop")) {
            return Collections.singletonList("rest_stop");
        }
        if (type.contains("parking")) {
            return Arrays.asList("parking", "parking_lot", "parking_garage");
        }
        if (type.contains("auto_parts") || type.contains("auto parts")) {
            return Collections.singletonList("auto_parts_store");
        }
        if (type.contains("towing") || type.contains("tow") || type.contains("workshop")) {
            return Collections.singletonList("car_repair");
        }
        if (type.contains("battery")) {
            return Arrays.asList("auto_parts_store", "car_repair");
        }
        if (type.contains("food") || type.contains("refreshment")) {
            return Arrays.asList("restaurant", "cafe", "convenience_store");
        }
        if (type.contains("lodging") || type.contains("accommodation") || type.contains("hotel")) {
            return Collections.singletonList("hotel");
        }
        if (type.contains("car_wash") || type.contains("car wash")) {
            return Collections.singletonList("car_wash");
        }
        if (type.contains("ebike") || type.contains("e_bike")) {
            return Arrays.asList("ebike_charging_station", "electric_vehicle_charging_station");
        }
        if (type.contains("atm")) {
            return Collections.singletonList("atm");
        }
        if (type.contains("medical")) {
            return Arrays.asList(
                    "hospital",
                    "general_hospital",
                    "medical_center",
                    "medical_clinic",
                    "doctor");
        }
        if (type.contains("breakdown")) {
            return Arrays.asList("car_repair", "tire_shop");
        }
        if (type.contains("repair")) {
            return Collections.singletonList("car_repair");
        }
        if (type.contains("fuel")) {
            return Collections.singletonList("gas_station");
        }
        if (type.contains("ev")) {
            return Collections.singletonList("electric_vehicle_charging_station");
        }
        if (type.contains("police")) {
            return Collections.singletonList("police");
        }
        if (type.contains("fire")) {
            return Collections.singletonList("fire_station");
        }
        return Arrays.asList("hospital", "general_hospital", "medical_center", "police", "fire_station");
    }

    static double searchRadiusMeters(String incidentType) {
        String type = incidentType == null ? "accident" : incidentType.toLowerCase(Locale.US);
        if (type.contains("accident") || type.contains("emergency")
                || type.contains("fire") || type.contains("police")
                || type.contains("medical") || type.contains("clinic")) {
            return EMERGENCY_SEARCH_RADIUS_METERS;
        }
        return DEFAULT_SEARCH_RADIUS_METERS;
    }

    static String normalizeCategory(String primaryType) {
        return normalizeCategory(primaryType, Collections.emptyList());
    }

    static String normalizeCategory(String primaryType, List<String> placeTypes) {
        String primaryCategory = normalizePlaceType(primaryType);
        String bestCategory = primaryCategory;
        int bestRank = categoryRank(bestCategory);
        if (placeTypes != null) {
            for (String placeType : placeTypes) {
                String category = normalizePlaceType(placeType);
                int rank = categoryRank(category);
                if (rank < bestRank) {
                    bestCategory = category;
                    bestRank = rank;
                }
            }
        }
        return bestCategory;
    }

    private static List<String> placeTypeNames(Place place) {
        List<String> typeNames = new ArrayList<>();
        if (place == null || place.getPlaceTypes() == null) {
            return typeNames;
        }
        for (String type : place.getPlaceTypes()) {
            if (type != null) {
                typeNames.add(type);
            }
        }
        return typeNames;
    }

    private static String normalizePlaceType(String primaryType) {
        String type = primaryType == null ? "aid" : primaryType.toLowerCase(Locale.US);
        if (type.contains("public_bath")) return "toilet";
        if (type.contains("car_wash")) return "car_wash";
        if (type.contains("medical_clinic") || type.contains("medical_center")) return "clinic";
        if (type.contains("medical_lab")) return "medical_lab";
        if (type.contains("pharmacy") || type.contains("drugstore")) return "pharmacy";
        if (type.contains("tire")) return "tire_shop";
        if (type.contains("rest_stop")) return "rest_stop";
        if (type.contains("parking")) return "parking";
        if (type.contains("auto_parts")) return "auto_parts";
        if (type.contains("restaurant") || type.contains("cafe") || type.contains("convenience")) return "food";
        if (type.contains("hotel") || type.contains("lodging")) return "lodging";
        if (type.contains("atm")) return "atm";
        if (type.contains("ebike") || type.contains("e_bike")) return "ev";
        if (type.contains("hospital") || type.contains("doctor")) return "medical";
        if (type.contains("police")) return "police";
        if (type.contains("fire")) return "fire";
        if (type.contains("gas")) return "fuel";
        if (type.contains("electric")) return "ev";
        if (type.contains("repair")) return "repair";
        return type;
    }

    private static int categoryRank(String category) {
        if (category == null || category.length() == 0) return 100;
        if (category.contains("police")) return 1;
        if (category.contains("fire")) return 2;
        if (category.contains("medical") || category.contains("clinic") || category.contains("pharmacy")) return 3;
        if (category.contains("fuel") || category.contains("ev")) return 4;
        if (category.contains("repair") || category.contains("tire") || category.contains("auto_parts")) return 5;
        if (category.contains("atm") || category.contains("toilet") || category.contains("parking")) return 6;
        if (category.contains("food") || category.contains("lodging") || category.contains("rest_stop")) return 7;
        if (category.contains("point_of_interest") || category.contains("establishment")
                || category.contains("local_government") || category.equals("aid")) return 90;
        return 50;
    }

    private static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        float[] result = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, result);
        return result[0] / 1000.0;
    }

    private static String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            return Math.max(50, Math.round(distanceKm * 1000.0)) + " m";
        }
        return String.format(Locale.US, "%.1f km", distanceKm);
    }

    private static String estimateEta(double distanceKm) {
        return "Route time in Maps";
    }

    private static double numericDistance(String value) {
        if (value == null) return Double.MAX_VALUE;
        try {
            String normalized = value.toLowerCase(Locale.US).replace("km", "").replace("m", "").trim();
            double amount = Double.parseDouble(normalized);
            return value.toLowerCase(Locale.US).contains("km") ? amount : amount / 1000.0;
        } catch (NumberFormatException ignored) {
            return Double.MAX_VALUE;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static Exception asException(Exception error) {
        return error;
    }
}
