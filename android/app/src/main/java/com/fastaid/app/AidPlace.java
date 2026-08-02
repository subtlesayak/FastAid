package com.fastaid.app;

final class AidPlace {
    final String id;
    final String name;
    final String category;
    final String distance;
    final String eta;
    final String phone;
    final String address;
    final String provider;
    final String openText;
    final double latitude;
    final double longitude;
    final boolean verified;
    final boolean openKnown;
    final boolean openNow;

    AidPlace(
            String id,
            String name,
            String category,
            String distance,
            String eta,
            String phone,
            String address,
            String provider,
            String openText,
            double latitude,
            double longitude,
            boolean verified,
            boolean openKnown,
            boolean openNow
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.distance = distance;
        this.eta = eta;
        this.phone = phone;
        this.address = address;
        this.provider = provider;
        this.openText = openText;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verified = verified;
        this.openKnown = openKnown;
        this.openNow = openNow;
    }

    boolean hasPhone() {
        return phone != null && phone.trim().length() > 0;
    }

    boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }
}
