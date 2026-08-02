# FastAid Backend API

## `GET /api/health`

Returns process liveness and the configured deployment, Maps, and notification modes. This endpoint remains `200` while the server process can answer requests.

```json
{
  "ok": true,
  "service": "FastAid backend",
  "mode": "local",
  "mapsProvider": "mock",
  "notificationProvider": "local"
}
```

## `GET /api/readiness`

Evaluates storage, HTTPS public URL, server Maps key, administrator bootstrap, and notification mode. In `pilot` mode missing required configuration returns `503`; a ready configuration returns `200`. Details never include secret values.

## `GET /api/countries`

Returns country emergency configuration from `prototype/country-config.json`.

## `GET /api/nearby/aid`

Query params:

```txt
incidentType=Accident | Medical | Breakdown | Fuel | Police | Fire | SOS | Tyre | Clinic | Pharmacy | Toilet | Rest_stop | Parking | Medical_lab | Auto_parts
type=hospital | general_hospital | medical_center | medical_clinic | doctor | pharmacy | drugstore | police | fire_station | gas_station | car_repair | tire_shop | auto_parts_store | electric_vehicle_charging_station | towing | public_bathroom | public_bath | rest_stop | parking | parking_lot | parking_garage | medical_lab
lat=28.6328
lng=77.2197
```

Example:

```txt
/api/nearby/aid?incidentType=Breakdown
```

Important response fields:

```json
{
  "provider": "mock",
  "incidentType": "Breakdown",
  "results": [
    {
      "name": "FastAid Tow Partner",
      "type": "towing",
      "verified": true,
      "source": "verified_responder"
    },
    {
      "name": "Speedy Auto Care",
      "type": "car_repair",
      "verified": false,
      "source": "public_place"
    }
  ]
}
```

`source` is intentionally explicit:

1. `verified_responder`: FastAid can notify this provider.
2. `public_place`: Maps data only; the app can call or route, but not promise dispatch.

## `POST /api/incidents`

Creates a persisted incident and starts ranked responder assignment. This emergency path remains public but is bounded by per-client rate limiting, request-size limits, and constrained input fields.

Request body:

```json
{
  "type": "Accident",
  "peopleCount": 3,
  "locationText": "Connaught Place, New Delhi",
  "note": "Car accident near metro gate"
}
```

## Authentication

### `POST /api/auth/signup`

Creates a `user` or `responder` account and returns a short-lived bearer token. Public requests cannot create administrators.

### `POST /api/auth/login`

Returns an opaque bearer token and its expiry. Passwords use salted `scrypt` credentials and the persisted session contains only a SHA-256 token hash.

### `POST /api/auth/logout`

Revokes the current bearer session. Requires `Authorization: Bearer <token>`.

### `GET /api/me`

Returns the authenticated user without credential fields.

## Protected Responder Decisions

### `POST /api/responder-alerts/:id/accept`

The assigned responder or an administrator can accept an alert and move its incident to `en_route`.

### `POST /api/responder-alerts/:id/decline`

The assigned responder or an administrator can decline an alert and move its incident to `searching_next_responder`.

Both routes require a bearer session and write alert and incident audit records.

## Persistent Pilot APIs

### `GET /api/incidents`

Lists persisted incidents newest first for administrators only.

### `GET /api/incidents/:id`

Returns one persisted incident to an administrator or its assigned responder. Other clients receive `401`/`403`.

### `PATCH /api/incidents/:id`

Updates the incident to a validated lifecycle `status`. Requires an assigned responder or administrator session and writes an audit event.

### `GET /api/responders`

Lists responder profiles for administrators only. Seeded demo partners are verified; self-onboarded responders begin pending.

### `POST /api/responders`

Creates one pending responder profile for the authenticated responder account. Required fields are `name` and `responderType`.

### `PATCH /api/responders/:id/availability`

Lets a verified responder publish `available`, `busy`, or `offline` for their own profile, optionally with a new location. Administrators may also perform this operation.

### `GET /api/responders/:id/alerts`

Lists alerts assigned to the authenticated responder's own profile. Administrators may inspect any responder.

### `PATCH /api/admin/responders/:id/verification`

Lets an administrator set `pending`, `verified`, `rejected`, or `suspended`. An optional bounded `reason` is included in the append-only audit event.

### `GET /api/admin/duplicate-suggestions`

Lists review-only possible duplicate incident pairs for administrators. Suggestions use a bounded time window plus nearby coordinates or the same specific location label. The response explicitly sets `automaticMerge` to `false`; this endpoint never changes incident state.

### `GET /api/admin/audit`

Lists append-only protected-operation events for administrators. The optional `limit` query is capped at 500.

## Local Data

Pilot state is atomically stored in `backend/data/fastaid-store.json` unless `FASTAID_DATA_FILE` overrides the path. This file is ignored by source control. Configure `FASTAID_ADMIN_EMAIL` and `FASTAID_ADMIN_PASSWORD` together to seed the first administrator, and optionally set `FASTAID_SESSION_TTL_MINUTES` (default 60, constrained to 5-1440 minutes).

Production deployment should replace the local adapter with a managed database while preserving the API contract. Raw passwords and raw bearer tokens are never written to the pilot store.

## Matching And Notification Delivery

Incident creation ranks only responder-store records that are verified, available, service-compatible, located within their service radius, and not previously attempted for that incident. Public Google Places results never enter this pipeline.

Service-type order from `incidentToTypes` is the primary rank; haversine distance is the secondary rank. Assignment reserves the selected responder as `busy`, creates one expiring `notified` alert, and persists each notification attempt.

- Decline marks the current alert `declined`, releases the responder, and assigns the next candidate.
- Timeout marks the current alert `timed_out`, releases the responder, and assigns the next candidate.
- Notification failure retries up to `FASTAID_NOTIFICATION_MAX_ATTEMPTS`, then releases and advances.
- Resolving, cancelling, or failing an incident closes its active alert and releases its responder.

The default `local` adapter is deterministic for development. The `firebase` adapter exposes the same `sendResponderAlert` contract for a future injected Firebase Admin sender; setting it without a sender fails closed and records each failed attempt.

### `GET /api/admin/notifications`

Returns persisted notification delivery attempts for administrators.
