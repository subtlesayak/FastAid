# FastAid Backend Stub

This folder is a planning stub for the backend service that will eventually power the prototype.

## Responsibilities

1. Store users, responders, partners, incidents, and audit logs.
2. Call Google Places, Place Details, Routes, and Geocoding with the server key.
3. Match incidents to verified responders.
4. Send push/SMS/call notifications.
5. Track incident state and responder state.
6. Keep country-specific emergency behavior configurable.

## Environment

Use the root `.env.example` file as the template.

Important:

```env
GOOGLE_MAPS_SERVER_KEY=
```

This key belongs only on the backend.

## Suggested First Backend Endpoints

```txt
POST /incidents
GET  /incidents/:id
GET  /nearby/aid
POST /incidents/:id/notify
POST /responder-alerts/:id/accept
POST /responder-alerts/:id/decline
PATCH /responders/me/location
PATCH /responders/me/availability
```

## First Implementation Slice

1. Build `/nearby/aid` with mock data.
2. Replace mock POI lookup with Google Places.
3. Add a verified responder table.
4. Merge public POIs and verified responders in one ranked response.
5. Keep the response field `source` as either `verified_responder` or `public_place`.
