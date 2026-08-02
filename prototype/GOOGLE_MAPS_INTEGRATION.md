# Google Maps Integration Notes

FastAid should use Google Maps data for location awareness and nearby aid discovery, not as the sole emergency dispatch system.

## Recommended API Split

Client app:

1. Maps SDK / Maps JavaScript API for map display
2. Restricted public key only
3. No Places or Routes server key in browser code

Backend:

1. Places API Nearby Search
2. Place Details
3. Routes API
4. Reverse Geocoding
5. Country configuration
6. Responder matching

## Nearby Aid Query Flow

1. App sends user latitude, longitude, incident type, and country code to backend.
2. Backend maps incident type to POI categories.
3. Backend calls Places Nearby Search for relevant categories.
4. Backend calls Routes API for distance and ETA.
5. Backend merges public POIs with verified FastAid responders.
6. App displays:
   - Verified responders: can be requested through FastAid.
   - Public places: can be called or navigated to, but are not automatically dispatched.

## Incident Type To POI Mapping

Accident:

1. `hospital`
2. `police`
3. `fire_station`

Medical:

1. `hospital`
2. `medical_center`
3. `medical_clinic`
4. `pharmacy`

Breakdown:

1. `car_repair`
2. `tire_shop`
3. `gas_station`
4. `parking`

Fuel:

1. `gas_station`
2. `electric_vehicle_charging_station`

Fire:

1. `fire_station`
2. `hospital`

Police:

1. `police`
2. `hospital`

## Backend Pseudocode

```js
async function findNearbyAid({ lat, lng, incidentType, countryCode }) {
  const country = countryConfig[countryCode];
  const categories = mapIncidentToPlaceTypes(incidentType, country);

  const publicPlaces = await placesProvider.searchNearby({
    location: { lat, lng },
    includedTypes: categories,
    radiusMeters: 5000
  });

  const verifiedResponders = await responderRepository.findAvailableNearby({
    lat,
    lng,
    incidentType,
    radiusKm: 10
  });

  return rankAidOptions({
    publicPlaces,
    verifiedResponders,
    origin: { lat, lng }
  });
}
```

## Key Rule

Public place data can answer: "What useful places are near me?"

Verified responder data can answer: "Who can FastAid notify to come help?"

Do not merge those two promises in the UI.
