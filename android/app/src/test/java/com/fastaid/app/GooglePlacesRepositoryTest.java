package com.fastaid.app;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class GooglePlacesRepositoryTest {
    @Test
    public void urgentNearbyCategoriesUseSpecificGooglePlaceTypes() {
        assertEquals(Arrays.asList("hospital", "general_hospital", "medical_center", "police", "fire_station"),
                GooglePlacesRepository.includedTypes("accident"));
        assertEquals(Collections.singletonList("tire_shop"),
                GooglePlacesRepository.includedTypes("tyre"));
        assertEquals(Arrays.asList("medical_clinic", "medical_center", "doctor"),
                GooglePlacesRepository.includedTypes("clinic"));
        assertEquals(Arrays.asList("pharmacy", "drugstore"),
                GooglePlacesRepository.includedTypes("pharmacy"));
        assertEquals(Arrays.asList("public_bathroom", "public_bath"),
                GooglePlacesRepository.includedTypes("toilet"));
        assertEquals(Collections.singletonList("rest_stop"),
                GooglePlacesRepository.includedTypes("rest_stop"));
        assertEquals(Arrays.asList("parking", "parking_lot", "parking_garage"),
                GooglePlacesRepository.includedTypes("parking"));
        assertEquals(Collections.singletonList("medical_lab"),
                GooglePlacesRepository.includedTypes("medical_lab"));
        assertEquals(Collections.singletonList("auto_parts_store"),
                GooglePlacesRepository.includedTypes("auto_parts"));
        assertEquals(Collections.singletonList("car_repair"),
                GooglePlacesRepository.includedTypes("towing"));
        assertEquals(Collections.singletonList("car_repair"),
                GooglePlacesRepository.includedTypes("workshop"));
        assertEquals(Arrays.asList("auto_parts_store", "car_repair"),
                GooglePlacesRepository.includedTypes("battery"));
        assertEquals(Arrays.asList("restaurant", "cafe", "convenience_store"),
                GooglePlacesRepository.includedTypes("food"));
        assertEquals(Collections.singletonList("hotel"),
                GooglePlacesRepository.includedTypes("lodging"));
        assertEquals(Collections.singletonList("car_wash"),
                GooglePlacesRepository.includedTypes("car_wash"));
        assertEquals(Arrays.asList("ebike_charging_station", "electric_vehicle_charging_station"),
                GooglePlacesRepository.includedTypes("ebike"));
        assertEquals(Collections.singletonList("atm"),
                GooglePlacesRepository.includedTypes("atm"));
        assertEquals(Collections.singletonList("gas_station"),
                GooglePlacesRepository.includedTypes("fuel"));
        assertEquals(Collections.singletonList("electric_vehicle_charging_station"),
                GooglePlacesRepository.includedTypes("ev"));
        assertEquals(Collections.singletonList("fire_station"),
                GooglePlacesRepository.includedTypes("fire"));
    }

    @Test
    public void emergencyCategoriesUseWiderSearchRadius() {
        assertEquals(15000.0, GooglePlacesRepository.searchRadiusMeters("fire"), 0.01);
        assertEquals(15000.0, GooglePlacesRepository.searchRadiusMeters("accident"), 0.01);
        assertEquals(5000.0, GooglePlacesRepository.searchRadiusMeters("fuel"), 0.01);
    }

    @Test
    public void sortsOpenThenUnknownThenClosedBeforeDistance() {
        AidPlace closed = place("Closed", "100 m", true, false, true);
        AidPlace unknown = place("Unknown", "8.0 km", false, false, false);
        AidPlace open = place("Open", "12.0 km", true, true, false);
        List<AidPlace> places = new ArrayList<>(Arrays.asList(closed, unknown, open));

        GooglePlacesRepository.sortPlaces(places);

        assertEquals("Open", places.get(0).name);
        assertEquals("Unknown", places.get(1).name);
        assertEquals("Closed", places.get(2).name);
    }

    @Test
    public void sortsEqualAvailabilityByPhoneThenDistanceThenName() {
        AidPlace oneKmNoPhone = place("Zulu", "1.0 km", true, true, false);
        AidPlace fiveHundredMetres = place("Bravo", "500 m", true, true, false);
        AidPlace oneKmWithPhone = place("Alpha", "1.0 km", true, true, true);
        List<AidPlace> places = new ArrayList<>(Arrays.asList(
                oneKmNoPhone, fiveHundredMetres, oneKmWithPhone));

        GooglePlacesRepository.sortPlaces(places);

        assertEquals("Alpha", places.get(0).name);
        assertEquals("Bravo", places.get(1).name);
        assertEquals("Zulu", places.get(2).name);
    }

    @Test
    public void accidentSortPrefersEmergencyAidOverVehicleCommerce() {
        AidPlace repair = place("Car accessory shop", "200 m", true, true, true, "repair");
        AidPlace hospital = place("City Hospital", "1.2 km", true, true, true, "medical");
        List<AidPlace> places = new ArrayList<>(Arrays.asList(repair, hospital));

        GooglePlacesRepository.sortPlaces(places, "accident");

        assertEquals("City Hospital", places.get(0).name);
        assertEquals("CHECK CATEGORY", ServiceQualityScanner.label("accident", repair));
    }

    @Test
    public void scannerFlagsSpecialtyHospitalsForAccidentCheck() {
        AidPlace eyeHospital = place("Eye Hospital", "600 m", true, true, true, "medical");

        assertEquals("CHECK FIRST", ServiceQualityScanner.label("accident", eyeHospital));
        assertEquals(1, ServiceQualityScanner.relevanceRank("accident", eyeHospital));
    }

    @Test
    public void scannerDowngradesFireCategoryWithoutFireServiceName() {
        AidPlace engineering = place("Harsha Engineering Works", "600 m", true, true, true, "fire");
        AidPlace station = place("Fire Station Peenya", "2.0 km", true, true, true, "fire");

        List<AidPlace> places = new ArrayList<>(Arrays.asList(engineering, station));
        GooglePlacesRepository.sortPlaces(places, "fire");

        assertEquals("Fire Station Peenya", places.get(0).name);
        assertEquals("CHECK FIRST", ServiceQualityScanner.label("fire", engineering));
    }

    @Test
    public void policeSortPrefersStationNamesOverLoosePoliceMatches() {
        AidPlace houseNumber = place("House Number 1818", "2.7 km", true, true, false, "police");
        AidPlace academy = place("APEX IAS ACADEMY", "3.9 km", true, false, true, "police");
        AidPlace sangha = place("Karnataka Police Maha Sangha", "3.9 km", false, false, true, "police");
        AidPlace station = place("Vidyaranyapura Police Station", "854 m", true, true, true, "police");
        List<AidPlace> places = new ArrayList<>(Arrays.asList(houseNumber, academy, sangha, station));

        GooglePlacesRepository.sortPlaces(places, "police");

        assertEquals("Vidyaranyapura Police Station", places.get(0).name);
        assertEquals("BEST MATCH", ServiceQualityScanner.label("police", station));
        assertEquals("CHECK FIRST", ServiceQualityScanner.label("police", houseNumber));
        assertEquals("CHECK FIRST", ServiceQualityScanner.label("police", academy));
        assertEquals("CHECK FIRST", ServiceQualityScanner.label("police", sangha));
    }

    @Test
    public void policeChowkiAndWomenStationRemainTrustedPoliceMatches() {
        AidPlace chowki = place("Police chowki", "2.1 km", true, true, false, "police");
        AidPlace womenStation = place("North East Women Police Station Yelahanka",
                "4.6 km", true, true, false, "police");

        assertEquals("BEST MATCH", ServiceQualityScanner.label("police", chowki));
        assertEquals("BEST MATCH", ServiceQualityScanner.label("police", womenStation));
    }

    @Test
    public void repairSortDowngradesCommerceNoiseBehindServicePlaces() {
        AidPlace showroom = place("Metro Car Accessories", "400 m", true, true, true, "repair");
        AidPlace academy = place("North Motor Academy", "300 m", true, true, true, "repair");
        AidPlace service = place("Speedy Auto Care Service", "1.2 km", true, true, true, "repair");
        List<AidPlace> places = new ArrayList<>(Arrays.asList(showroom, academy, service));

        GooglePlacesRepository.sortPlaces(places, "repair");

        assertEquals("Speedy Auto Care Service", places.get(0).name);
        assertEquals("CHECK FIRST", ServiceQualityScanner.label("repair", showroom));
        assertEquals("CHECK FIRST", ServiceQualityScanner.label("repair", academy));
    }

    private AidPlace place(
            String name, String distance, boolean openKnown, boolean openNow, boolean hasPhone) {
        return new AidPlace(
                name.toLowerCase(), name, "medical", distance, "Route time in Maps",
                hasPhone ? "12345" : "", "", "google_places_sdk",
                openKnown ? (openNow ? "Open now" : "Closed now") : "Hours unavailable",
                12.0, 77.0, false, openKnown, openNow);
    }

    private AidPlace place(
            String name, String distance, boolean openKnown, boolean openNow,
            boolean hasPhone, String category) {
        return new AidPlace(
                name.toLowerCase(), name, category, distance, "Route time in Maps",
                hasPhone ? "12345" : "", "", "google_places_sdk",
                openKnown ? (openNow ? "Open now" : "Closed now") : "Hours unavailable",
                12.0, 77.0, false, openKnown, openNow);
    }

    @Test
    public void returnedPlaceTypesKeepUsefulCategoryIdentity() {
        assertEquals("tire_shop", GooglePlacesRepository.normalizeCategory("tire_shop"));
        assertEquals("clinic", GooglePlacesRepository.normalizeCategory("medical_clinic"));
        assertEquals("pharmacy", GooglePlacesRepository.normalizeCategory("drugstore"));
        assertEquals("toilet", GooglePlacesRepository.normalizeCategory("public_bathroom"));
        assertEquals("rest_stop", GooglePlacesRepository.normalizeCategory("rest_stop"));
        assertEquals("parking", GooglePlacesRepository.normalizeCategory("parking_lot"));
        assertEquals("medical_lab", GooglePlacesRepository.normalizeCategory("medical_lab"));
        assertEquals("auto_parts", GooglePlacesRepository.normalizeCategory("auto_parts_store"));
        assertEquals("food", GooglePlacesRepository.normalizeCategory("restaurant"));
        assertEquals("food", GooglePlacesRepository.normalizeCategory("convenience_store"));
        assertEquals("lodging", GooglePlacesRepository.normalizeCategory("hotel"));
        assertEquals("car_wash", GooglePlacesRepository.normalizeCategory("car_wash"));
        assertEquals("ev", GooglePlacesRepository.normalizeCategory("ebike_charging_station"));
        assertEquals("atm", GooglePlacesRepository.normalizeCategory("atm"));
    }

    @Test
    public void returnedPlaceTypesRecoverSpecificCategoryWhenPrimaryTypeIsGeneric() {
        assertEquals("police", GooglePlacesRepository.normalizeCategory(
                "point_of_interest", Arrays.asList("establishment", "police")));
        assertEquals("fire", GooglePlacesRepository.normalizeCategory(
                "local_government_office", Arrays.asList("point_of_interest", "fire_station")));
        assertEquals("repair", GooglePlacesRepository.normalizeCategory(
                "point_of_interest", Arrays.asList("car_repair", "establishment")));
        assertEquals("atm", GooglePlacesRepository.normalizeCategory(
                "establishment", Arrays.asList("finance", "atm")));
    }

    @Test
    public void scannerAcceptsExtendedNearbyAidCategories() {
        assertEquals("BEST MATCH", ServiceQualityScanner.label("atm",
                place("Nearby ATM", "50 m", true, true, true, "atm")));
        assertEquals("BEST MATCH", ServiceQualityScanner.label("food",
                place("Highway Cafe", "200 m", true, true, true, "food")));
        assertEquals("BEST MATCH", ServiceQualityScanner.label("lodging",
                place("Rest Hotel", "300 m", true, true, true, "lodging")));
        assertEquals("BEST MATCH", ServiceQualityScanner.label("car_wash",
                place("Quick Car Wash", "500 m", true, true, true, "car_wash")));
        assertEquals("BEST MATCH", ServiceQualityScanner.label("battery",
                place("Battery and Parts", "500 m", true, true, true, "auto_parts")));
        assertEquals("BEST MATCH", ServiceQualityScanner.label("workshop",
                place("Vehicle Workshop", "500 m", true, true, true, "repair")));
    }
}
