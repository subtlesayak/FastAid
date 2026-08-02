package com.fastaid.app;

import java.util.Locale;

final class ServiceQualityScanner {
    private ServiceQualityScanner() {
    }

    static int relevanceRank(String incidentType, AidPlace place) {
        if (isRelevant(incidentType, place)) {
            return needsManualCheck(incidentType, place) ? 1 : 0;
        }
        return 2;
    }

    static int score(AidPlace place) {
        int score = 45;
        if (place.openKnown && place.openNow) score += 25;
        else if (place.openKnown) score -= 25;
        if (place.hasPhone()) score += 12;
        if (place.hasCoordinates()) score += 8;
        double distance = numericDistanceKm(place.distance);
        if (distance <= 1.0) score += 10;
        else if (distance <= 3.0) score += 6;
        else if (distance <= 7.0) score += 2;
        else score -= 5;
        if (isSpecialtyMedical(place)) score -= 18;
        return Math.max(0, Math.min(100, score));
    }

    static String label(String incidentType, AidPlace place) {
        if (!isRelevant(incidentType, place)) return "CHECK CATEGORY";
        if (needsManualCheck(incidentType, place)) return "CHECK FIRST";
        int score = score(place);
        if (score >= 75) return "BEST MATCH";
        if (score >= 58) return "GOOD OPTION";
        return "CHECK FIRST";
    }

    static boolean isRelevant(String incidentType, AidPlace place) {
        String type = normalize(incidentType);
        String category = normalize(place == null ? "" : place.category);
        if (type.contains("accident") || type.contains("emergency")) {
            return hasAny(category, "medical", "clinic", "hospital", "police", "fire");
        }
        if (type.contains("medical")) {
            return hasAny(category, "medical", "clinic", "hospital");
        }
        if (type.contains("clinic")) return category.contains("clinic");
        if (type.contains("pharmacy")) return category.contains("pharmacy");
        if (type.contains("medical_lab") || type.equals("lab")) return category.contains("medical_lab");
        if (type.contains("police")) return category.contains("police");
        if (type.contains("fire")) return category.contains("fire");
        if (type.contains("breakdown")) return hasAny(category, "repair", "tire");
        if (type.contains("repair")) return category.contains("repair");
        if (type.contains("tyre") || type.contains("tire")) return category.contains("tire");
        if (type.contains("fuel")) return category.contains("fuel");
        if (type.contains("ev")) return category.contains("ev");
        if (type.contains("toilet")) return category.contains("toilet");
        if (type.contains("rest_stop")) return category.contains("rest_stop");
        if (type.contains("parking")) return category.contains("parking");
        if (type.contains("auto_parts")) return category.contains("auto_parts");
        if (type.contains("towing") || type.contains("workshop")) return category.contains("repair");
        if (type.contains("battery")) return hasAny(category, "auto_parts", "repair");
        if (type.contains("food")) return category.contains("food");
        if (type.contains("lodging")) return category.contains("lodging");
        if (type.contains("car_wash")) return category.contains("car_wash");
        if (type.contains("ebike")) return category.contains("ev");
        if (type.contains("atm")) return category.contains("atm");
        return true;
    }

    private static boolean isAccidentOrMedical(String incidentType) {
        String type = normalize(incidentType);
        return type.contains("accident") || type.contains("emergency")
                || type.contains("medical") || type.contains("clinic");
    }

    private static boolean needsManualCheck(String incidentType, AidPlace place) {
        String type = normalize(incidentType);
        String name = normalize(place == null ? "" : place.name + " " + place.address);
        if (isSpecialtyMedical(place) && isAccidentOrMedical(type)) return true;
        if (type.contains("fire")) {
            return !isLikelyFireService(name);
        }
        if (type.contains("police")) {
            return !isLikelyPoliceService(name);
        }
        if (isVehicleHelp(type)) {
            return isLikelyVehicleCommerceTrap(name);
        }
        return false;
    }

    private static boolean isLikelyPoliceService(String name) {
        if (!name.contains("police")) return false;
        return hasAny(name, "station", "chowki", "chowk", "outpost", "check_post",
                "traffic", "law_and_order", "mahil", "women");
    }

    private static boolean isLikelyFireService(String name) {
        if (!name.contains("fire")) return false;
        return hasAny(name, "station", "brigade", "service", "services", "emergency");
    }

    private static boolean isVehicleHelp(String type) {
        return type.contains("breakdown") || type.contains("repair") || type.contains("towing")
                || type.contains("workshop") || type.contains("battery")
                || type.contains("tyre") || type.contains("tire")
                || type.contains("auto_parts");
    }

    private static boolean isLikelyVehicleCommerceTrap(String name) {
        boolean hasServiceSignal = hasAny(name, "repair", "service", "services", "workshop",
                "garage", "towing", "tow", "puncture", "tyre", "tire", "battery", "mechanic",
                "auto_care", "motors");
        if (hasServiceSignal) return false;
        return hasAny(name, "showroom", "dealer", "dealers", "sales", "accessory", "accessories",
                "academy", "school", "college", "digital", "house_number", "apartment",
                "real_estate", "finance");
    }

    private static boolean isSpecialtyMedical(AidPlace place) {
        if (place == null) return false;
        String name = normalize(place.name + " " + place.address);
        return hasAny(name, "eye", "dental", "skin", "cosmetic", "ivf", "fertility",
                "veterinary", "pet", "optical", "diagnostic", "scan centre", "imaging");
    }

    private static boolean hasAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static double numericDistanceKm(String value) {
        if (value == null) return Double.MAX_VALUE;
        try {
            String normalized = value.toLowerCase(Locale.US).replace("km", "").replace("m", "").trim();
            double amount = Double.parseDouble(normalized);
            return value.toLowerCase(Locale.US).contains("km") ? amount : amount / 1000.0;
        } catch (NumberFormatException ignored) {
            return Double.MAX_VALUE;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US).replace(' ', '_');
    }
}
