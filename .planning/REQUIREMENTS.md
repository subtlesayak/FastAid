# Requirements: FastAid v1.1 Pilot Backend Foundations

**Defined:** 2026-07-13
**Core Value:** A user can quickly find relevant nearby aid and create a trustworthy request without confusing public map places with verified responders.

## v1.1 Requirements

### Persistence

- [x] **PERS-01**: Incidents and lifecycle status survive backend restarts.
- [x] **PERS-02**: Responder profiles, verification state, and availability survive backend restarts.
- [x] **PERS-03**: Responder alerts remain linked to their incident and assigned responder.

### Incident API

- [x] **INCD-01**: Client can create, list, and retrieve incidents.
- [x] **INCD-02**: Authorized actors can advance an incident through validated lifecycle states.
- [x] **INCD-03**: Every incident and status change records creation and update timestamps.

### Responders

- [x] **RESP-01**: A responder can submit an onboarding profile without becoming verified automatically.
- [x] **RESP-02**: Verified responders can publish available, busy, or offline status and location.
- [x] **RESP-03**: A responder can retrieve only alerts assigned to that responder.

### Authentication And Safety

- [x] **AUTH-01**: Protected responder and admin operations require a short-lived authenticated session.
- [x] **AUTH-02**: Password-equivalent secrets are salted and hashed at rest.
- [x] **SAFE-01**: Public POIs remain explicitly separate from verified dispatchable responders.
- [x] **SAFE-02**: Incident and responder status changes produce an append-only audit record.

### Matching And Notifications

- [x] **MATCH-01**: New incidents rank available verified responders by service match and distance.
- [x] **MATCH-02**: Declined or timed-out alerts move to the next eligible responder without duplicate active assignments.
- [x] **NOTF-01**: Notification delivery uses an adapter with a local test implementation and Firebase-ready interface.

### Pilot Deployment

- [x] **PILOT-01**: Backend configuration supports an HTTPS deployment URL without Android code changes.
- [x] **PILOT-02**: Health and readiness endpoints distinguish process health from external-provider readiness.
- [x] **PILOT-03**: Pilot data storage and secrets are excluded from source control and APK packaging.

## v1.2 Requirements

### Field Operations

- [x] **OPS-01**: An authenticated administrator can review responders and set pending, verified, rejected, or suspended status through an operations dashboard.
- [ ] **OPS-02**: A configured emergency contact can receive an incident status notification without exposing other profile data.
- [x] **OPS-03**: An authenticated pilot operator can review possible duplicate incidents without the system merging or closing them automatically.

### Remote Delivery

- [ ] **NOTF-02**: A verified responder can receive a real remote incident notification through a configured Firebase-compatible provider.

### Durable Runtime

- [ ] **DATA-01**: Incident, responder, session, audit, alert, and delivery-attempt records can use a managed database adapter while preserving the v1.1 API contract.
- [ ] **JOBS-01**: Alert expiry, retry, and next-responder work survives process restarts and executes at most once per claim.
- [ ] **OBS-01**: Operators can inspect health, readiness, notification failures, job backlog, and documented backup/restore evidence.

### Pilot Validation

- [ ] **UAT-01**: A physical Android device passes live Maps/Places, opening-state, Call, and Go checks using restricted production-style keys.
- [ ] **UAT-02**: Approved participants can complete one staged incident-to-responder exercise with an auditable outcome and rollback procedure.

## Out of Scope

| Feature | Reason |
|---------|--------|
| Official government dispatch integration | Requires agency agreements and region-specific certification. |
| Medical diagnosis or triage automation | Unsafe and outside the nearby-aid product boundary. |
| Payments, insurance, or claims | Not required to validate emergency discovery and responder coordination. |
| Guaranteed hospital availability | Google Places does not supply verified capacity data. |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PERS-01, PERS-02, PERS-03 | Phase 7 | Complete |
| INCD-01, INCD-02, INCD-03 | Phase 7 | Complete |
| RESP-01, RESP-02, RESP-03 | Phase 7 | Complete |
| AUTH-01, AUTH-02, SAFE-02 | Phase 8 | Complete |
| SAFE-01 | Phase 7 | Complete |
| MATCH-01, MATCH-02, NOTF-01 | Phase 9 | Complete |
| PILOT-01, PILOT-02, PILOT-03 | Phase 10 | Complete |
| OPS-01, OPS-03 | Phase 11 | Complete |
| OPS-02, NOTF-02 | Phase 12 | Planned |
| DATA-01, JOBS-01, OBS-01 | Phase 13 | Planned |
| UAT-01, UAT-02 | Phase 14 | Planned |

**Coverage:**

- v1.1 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0
- v1.2 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-07-13*
*Last updated: 2026-07-16 for v1.2 Pilot Operations*
