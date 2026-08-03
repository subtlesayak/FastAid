# Phase 9 Context: Matching And Notification Pipeline

## Boundary

Phase 9 turns an incident into one controlled responder assignment at a time. Google Places and other public POIs remain call/navigation results only and never enter dispatch.

## Decisions

- Rank only verified, available responder records whose service type is compatible, whose location is valid, and whose configured service radius covers the incident.
- Use the incident service-type order as the primary relevance rank and road-independent haversine distance as the secondary rank.
- Reserve a responder as `busy` as soon as an alert is assigned, preventing simultaneous active assignments.
- Persist every responder attempt as a separate alert; prior responder IDs are excluded from subsequent attempts.
- Permit at most one `notified` or `accepted` alert per incident.
- On decline, timeout, or exhausted notification retries, release the responder and synchronously advance to the next candidate.
- Use a deterministic local notification adapter in development and expose the same `sendResponderAlert` contract for an injected Firebase sender.
- Persist every delivery attempt so provider failures and retry counts remain inspectable.

## Timing

- Alert expiry defaults to 45 seconds and is configurable for controlled testing.
- A lightweight single-process sweep handles expiration for this pilot adapter.
- Phase 10 will document that production deployment needs a durable job/queue implementation.

## Deferred

- Multi-responder dispatch for one incident.
- Road-traffic ETA ranking.
- Government CAD/EMS integration.
- Firebase Admin SDK credentials and remote delivery.
