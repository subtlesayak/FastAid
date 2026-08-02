package com.fastaid.app;

final class EmergencyLocationPolicy {
    static final long SOS_MAX_AGE_MILLIS = 10L * 60L * 1000L;
    static final long NEARBY_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L;
    static final long MAX_FUTURE_SKEW_MILLIS = 2L * 60L * 1000L;

    private EmergencyLocationPolicy() {
    }

    static boolean hasValidCoordinates(double latitude, double longitude) {
        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }

    static boolean isFresh(long fixTimestamp, long now, long maxAgeMillis) {
        if (fixTimestamp <= 0L || fixTimestamp - now > MAX_FUTURE_SKEW_MILLIS) return false;
        long age = Math.max(0L, now - fixTimestamp);
        return age <= maxAgeMillis;
    }

    static boolean canSendSos(double latitude, double longitude, long fixTimestamp, long now) {
        return hasValidCoordinates(latitude, longitude)
                && isFresh(fixTimestamp, now, SOS_MAX_AGE_MILLIS);
    }

    static boolean canSearchNearby(double latitude, double longitude, long fixTimestamp, long now) {
        return hasValidCoordinates(latitude, longitude)
                && isFresh(fixTimestamp, now, NEARBY_MAX_AGE_MILLIS);
    }

    static boolean isBetterFix(
            long candidateTimestamp,
            float candidateAccuracy,
            long currentTimestamp,
            float currentAccuracy
    ) {
        if (currentTimestamp <= 0L) return true;
        long timeDelta = candidateTimestamp - currentTimestamp;
        if (timeDelta > 2L * 60L * 1000L) return true;
        if (timeDelta < -2L * 60L * 1000L) return false;
        if (Float.isNaN(currentAccuracy)) return true;
        if (Float.isNaN(candidateAccuracy)) return false;
        return candidateAccuracy < currentAccuracy;
    }
}
