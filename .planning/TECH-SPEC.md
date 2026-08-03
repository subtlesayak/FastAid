# FastAid Technical Specification

GSD Core Version: 1.9.1

## Architecture Overview

FastAid has four major layers:

1. Mobile app
2. Backend API
3. Maps and POI provider layer
4. Notification and responder dispatch layer

The maps layer finds nearby public places. The responder layer contacts verified people or partners. These must remain separate in the architecture.

## Suggested Stack

Mobile:

1. React Native or Flutter
2. Google Maps SDK or provider abstraction
3. Push notifications
4. Local storage for offline state

Backend:

1. Node.js, NestJS, or Express
2. PostgreSQL with PostGIS for geospatial queries
3. Redis for live incident and responder matching queues
4. WebSockets or managed realtime service
5. Object storage for media uploads

Notifications:

1. Firebase Cloud Messaging
2. SMS gateway
3. Optional automated voice call provider
4. Email only for non-urgent admin notifications

Maps and POI:

1. Google Maps SDK for display
2. Google Places API for nearby POIs
3. Google Routes API for ETA and distance
4. Provider interface for future Mapbox, OpenStreetMap, or local datasets

## Domain Model

### User

Fields:

1. id
2. name
3. phone
4. email
5. role
6. emergency_contacts
7. medical_profile
8. vehicle_profile
9. country_code
10. preferred_language

### Incident

Fields:

1. id
2. created_by
3. incident_type
4. location_lat
5. location_lng
6. address_text
7. severity
8. people_count
9. vehicle_type
10. media_urls
11. status
12. country_code
13. emergency_number_used
14. created_at
15. updated_at

Statuses:

1. created
2. verifying
3. notified
4. accepted
5. en_route
6. arrived
7. resolved
8. cancelled
9. failed

### Responder

Fields:

1. id
2. user_id
3. responder_type
4. verification_status
5. current_lat
6. current_lng
7. availability_status
8. service_radius_km
9. partner_org_id
10. last_seen_at

Types:

1. ambulance
2. hospital
3. police
4. fire
5. repair
6. towing
7. fuel
8. pharmacy
9. volunteer

### POI

Fields:

1. provider
2. provider_place_id
3. name
4. category
5. location_lat
6. location_lng
7. address
8. phone
9. open_now
10. rating
11. source_updated_at

Important: POIs are not automatically treated as verified responders.

### DispatchAttempt

Fields:

1. id
2. incident_id
3. responder_id
4. notification_channel
5. status
6. sent_at
7. accepted_at
8. declined_at
9. timeout_at

## POI Provider Interface

Required methods:

1. searchNearby(location, categories, radius)
2. getPlaceDetails(placeId)
3. getRoute(origin, destination, mode)
4. reverseGeocode(location)
5. getCountryConfig(location)

Category mapping:

Medical:

1. hospital
2. general_hospital
3. medical_center
4. medical_clinic
5. pharmacy

Official:

1. police
2. fire_station

Roadside:

1. gas_station
2. car_repair
3. tire_shop
4. electric_vehicle_charging_station
5. parking
6. rest_stop

## Incident Matching Logic

Ranking factors:

1. Responder verification status
2. Service type match
3. Distance
4. ETA
5. Availability
6. Current load
7. Last response time
8. User preference or partner priority

Fallback order:

1. Verified FastAid responders
2. Verified partner organizations
3. Nearby public POIs with call/directions only
4. Official emergency number
5. SMS SOS to emergency contacts

## API Endpoints

Auth:

1. POST /auth/signup
2. POST /auth/login
3. POST /auth/logout

User:

1. GET /me
2. PATCH /me
3. PATCH /me/emergency-contacts
4. PATCH /me/medical-profile
5. PATCH /me/vehicle-profile

Nearby:

1. GET /nearby/aid
2. GET /nearby/place/:id
3. GET /nearby/route

Incident:

1. POST /incidents
2. GET /incidents/:id
3. PATCH /incidents/:id
4. POST /incidents/:id/media
5. POST /incidents/:id/cancel
6. POST /incidents/:id/resolve

Responder:

1. PATCH /responders/me/availability
2. PATCH /responders/me/location
3. GET /responders/me/alerts
4. POST /responders/alerts/:id/accept
5. POST /responders/alerts/:id/decline
6. PATCH /responders/incidents/:id/status

Admin:

1. GET /admin/incidents
2. GET /admin/responders
3. POST /admin/partners
4. PATCH /admin/responders/:id/verify

## Offline Strategy

Store locally:

1. Last known location
2. Emergency contacts
3. Country emergency number
4. User medical profile
5. Draft incident report
6. Recently viewed nearby aid

Offline actions:

1. Call emergency number
2. Send SMS SOS with location if available
3. Queue incident report
4. Show cached map if available
5. Show last-known nearby places with stale-data warning

## Security And Privacy

Requirements:

1. Encrypt sensitive data in transit.
2. Restrict medical and location data access by role.
3. Store audit logs for incident access and status changes.
4. Use short-lived signed URLs for media.
5. Allow users to delete non-incident personal data.
6. Keep emergency incident logs according to legal policy.

## Country Configuration

Fields:

1. country_code
2. emergency_numbers
3. default_language
4. distance_unit
5. supported_poi_categories
6. official_dispatch_enabled
7. sms_sos_enabled
8. privacy_policy_url
9. local_terms_url

## Technical Risks

1. Google Places coverage varies by country.
2. Place phone numbers may be missing or stale.
3. Public POIs are not guaranteed emergency responders.
4. Low-network areas may prevent live tracking.
5. False alerts can waste resources.
6. Location accuracy may be poor indoors or on rural roads.
7. Emergency laws vary by country.

## Mitigations

1. Use verified partners for dispatch promises.
2. Keep official emergency call fallback.
3. Show confidence and verification labels.
4. Use SMS fallback.
5. Support duplicate incident detection.
6. Keep audit logs.
7. Configure emergency behavior per country.
