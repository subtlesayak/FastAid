# Phase 7 Context: Persistent Incident And Responder Store

## Goal

Replace volatile in-memory prototype state with durable pilot storage while preserving the existing Android and web API behavior.

## Decisions

- Keep the current dependency-free Node.js server for this local pilot slice.
- Store incidents, alerts, and responders behind a `JsonStore` adapter with atomic temporary-file replacement.
- Seed only explicitly verified mock partners as verified responders.
- Self-onboarded responders begin `pending` and `offline`; onboarding never grants verification.
- Preserve the strict separation between public Google Places POIs and dispatchable verified responders.
- Use a configurable `FASTAID_DATA_FILE` so tests and future adapters do not depend on the production-like local path.

## Scope

Included:

- Persistent incident, alert, and responder collections
- Incident create/list/get/status APIs
- Responder list/onboard/availability/assigned-alert APIs
- Existing accept/decline lifecycle migrated to persistent state
- Unit and process-restart integration tests

Deferred:

- Authentication and role authorization to Phase 8
- Audit log to Phase 8
- Real matching timeout and notification delivery to Phase 9
- Managed database and HTTPS deployment to Phase 10
