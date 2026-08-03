# Roadmap: FastAid

## Milestones

- [x] **v1.0 Android Nearby-Aid MVP** - Android phases A1-A6 shipped 2026-07-13
- [x] **v1.1 Pilot Backend Foundations** - Phases 7-10 completed and audited 2026-07-13
- [ ] **v1.2 Pilot Operations** - Phases 11-14 in progress
- [x] **The Fast Slate** - Focused Android nearby-aid repair phases for emergency UX trust, live place quality, accessibility, and device evidence (completed 2026-08-03)

## Overview

FastAid v1.1 turns the verified-responder prototype into a persistent and auditable pilot backend. Work proceeds from durable domain storage through authentication, responder matching and notification adapters, then HTTPS deployment readiness.

FastAid v1.2 makes that controlled pilot operable: administrators gain a protected console, real notification providers replace simulation, runtime state becomes durable across instances and restarts, and the complete flow is validated on-device and in a staged field exercise.

The Fast Slate is an urgent app-quality repair track layered over the existing roadmap. It keeps public nearby POIs discovery-only, avoids claiming official dispatch, and focuses the Android app on fast, trustworthy nearby aid for real users.

## Phases

- [x] **Phase 7: Persistent Incident And Responder Store** - Persist incidents, alerts, responders, and lifecycle APIs across restarts. (completed 2026-07-12)
- [x] **Phase 8: Authentication And Audit Safety** - Protect responder/admin operations and log sensitive state changes. (completed 2026-07-12)
- [x] **Phase 9: Matching And Notification Pipeline** - Rank verified responders and deliver retryable alert notifications. (completed 2026-07-12)
- [x] **Phase 10: HTTPS Pilot Readiness** - Configure deployable URLs, readiness checks, secrets, and field UAT. (completed 2026-07-12)
- [x] **Phase 11: Partner Operations Console** - Authenticate pilot operators and expose responder verification, incident review, and audit workflows. (completed 2026-07-16)
- [ ] **Phase 12: Remote Notification Delivery** - Deliver responder and emergency-contact notifications through configured providers.
- [ ] **Phase 13: Durable Pilot Runtime** - Add managed persistence, durable jobs, monitoring, and backup/restore procedures.
- [ ] **Phase 14: Live Device And Field UAT** - Complete restricted-key device checks and one staged, auditable responder exercise.
- [x] **Phase FS1: Emergency Surface Trust** - Restructure SOS, official emergency numbers, stale demo profile state, and emergency copy for first-glance use. (completed 2026-08-03)
- [x] **Phase FS2: Nearby Aid Quality Slate** - Tighten category matching, scanner labels, result grouping, open/callable state, and Call/Go confidence. (completed 2026-08-03)
- [x] **Phase FS3: Map-First Live Context** - Make the Map tab a 90% full-screen task surface with selected-place details and uncluttered emergency controls. (completed 2026-08-03)
- [x] **Phase FS4: Safety Profile Material 3** - Complete the profile into grouped emergency identity, medical, contacts, vehicle, language, and privacy sections. (completed 2026-08-03)
- [x] **Phase FS5: Accessibility And Localization Sweep** - Verify TalkBack, touch targets, text scale, language switching through Malayalam, and country emergency numbers. (completed 2026-08-03)
- [x] **Phase FS6: Connected-Device Verification** - Run APK/device smoke checks for every public-user function and capture screenshot/XML evidence. (completed 2026-08-03)

## Phase Details

### Phase 7: Persistent Incident And Responder Store

**Goal**: Replace volatile maps with a durable local pilot store and complete incident/responder lifecycle APIs.
**Depends on**: v1.0 Android MVP
**Requirements**: PERS-01, PERS-02, PERS-03, INCD-01, INCD-02, INCD-03, RESP-01, RESP-02, RESP-03, SAFE-01
**Success Criteria**:

  1. An incident created through HTTP still exists after the backend process restarts.
  2. Clients can create, list, retrieve, and update validated incident states.
  3. Responder onboarding starts pending and responder availability persists.
  4. Alerts remain linked to incidents and assigned verified responders.

**Plans**: 1 plan

Plans:

- [x] 07-01: Implement atomic JSON persistence and lifecycle APIs with restart verification.

### Phase 8: Authentication And Audit Safety

**Goal**: Add sessions, secure credential storage, role authorization, and append-only audit records.
**Depends on**: Phase 7
**Requirements**: AUTH-01, AUTH-02, SAFE-02
**Success Criteria**:

  1. Unauthenticated clients cannot change responder or admin-controlled state.
  2. Authentication secrets are salted and hashed at rest.
  3. Every protected lifecycle change records actor, action, target, and timestamp.

**Plans**: 1 plan

Plans:

- [x] 08-01: Define and implement authentication and authorization boundary.

### Phase 9: Matching And Notification Pipeline

**Goal**: Match incidents to eligible verified responders and deliver alerts through a provider adapter.
**Depends on**: Phase 8
**Requirements**: MATCH-01, MATCH-02, NOTF-01
**Success Criteria**:

  1. Matching excludes public POIs and unavailable or unverified responders.
  2. Decline and timeout advance to the next responder without duplicate active alerts.
  3. Notification delivery works through a deterministic local adapter and a Firebase-ready contract.

**Plans**: 1 plan

Plans:

- [x] 09-01: Implement matching, assignment state, timeout, and notification adapter.

### Phase 10: HTTPS Pilot Readiness

**Goal**: Prepare the backend and Android configuration for a controlled remote pilot.
**Depends on**: Phase 9
**Requirements**: PILOT-01, PILOT-02, PILOT-03
**Success Criteria**:

  1. Android can target a configured HTTPS API without source edits.
  2. Health and readiness endpoints report distinct process and provider states.
  3. Secrets and pilot data are excluded from source and APK artifacts.
  4. A documented field UAT verifies one end-to-end responder journey.

**Plans**: 1 plan

Plans:

- [x] 10-01: Add deployment configuration, readiness checks, and pilot UAT guide.

### Phase 11: Partner Operations Console

**Goal**: Turn the dispatcher shell into a protected, accessible operations workspace.
**Depends on**: Phase 10
**Requirements**: OPS-01, OPS-03
**Success Criteria**:

  1. An administrator can sign in and inspect incidents, responders, audit events, and provider readiness.
  2. An administrator can verify, reject, or suspend a responder and see the persisted result.
  3. Operators can review possible duplicate incidents without automatic merging or destructive action.
  4. Keyboard, screen-reader, responsive, loading, empty, and error states are present.

**Plans**: 1 plan

Plans:

- [x] 11-01: Implement authenticated partner operations console and focused verification tests.

### Phase 12: Remote Notification Delivery

**Goal**: Replace simulated delivery with configured remote responder and emergency-contact notifications.
**Depends on**: Phase 11
**Requirements**: OPS-02, NOTF-02
**Success Criteria**:

  1. A configured remote provider receives responder alerts with bounded retries and persisted outcomes.
  2. Emergency-contact messages contain only the incident status and explicitly consented location fields.
  3. Missing or failed providers remain visible and fail closed without false delivery claims.

**Plans**: 1 plan

Plans:

- [ ] 12-01: Integrate provider delivery, consent boundaries, and failure recovery.

### Phase 13: Durable Pilot Runtime

**Goal**: Make pilot state, scheduled work, and operational evidence resilient beyond one process.
**Depends on**: Phase 12
**Requirements**: DATA-01, JOBS-01, OBS-01
**Success Criteria**:

  1. A managed database adapter preserves the existing API and lifecycle behavior.
  2. Alert expiry and retry work resumes after process restart without duplicate claims.
  3. Operators can inspect backlog and delivery health and demonstrate backup restoration.

**Plans**: 1 plan

Plans:

- [ ] 13-01: Add managed persistence, durable jobs, metrics, and recovery verification.

### Phase 14: Live Device And Field UAT

**Goal**: Validate real Google data and one controlled end-to-end responder journey.
**Depends on**: Phase 13
**Requirements**: UAT-01, UAT-02
**Success Criteria**:

  1. Restricted keys load live Maps and Places on the physical Android device.
  2. Opening state, phone availability, distance, Call, and Go actions match live place data.
  3. Approved participants complete one staged request, notification, acceptance, arrival, and resolution flow.
  4. Audit evidence and rollback steps are recorded without claiming official dispatch.

**Plans**: 1 plan

Plans:

- [ ] 14-01: Execute live-key device and staged field UAT with evidence.

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 7. Persistent Incident And Responder Store | v1.1 | 1/1 | Complete    | 2026-07-12 |
| 8. Authentication And Audit Safety | v1.1 | 1/1 | Complete    | 2026-07-12 |
| 9. Matching And Notification Pipeline | v1.1 | 1/1 | Complete    | 2026-07-12 |
| 10. HTTPS Pilot Readiness | v1.1 | 1/1 | Complete    | 2026-07-12 |
| 11. Partner Operations Console | v1.2 | 1/1 | Complete | 2026-07-16 |
| 12. Remote Notification Delivery | v1.2 | 0/1 | Not started | - |
| 13. Durable Pilot Runtime | v1.2 | 0/1 | Not started | - |
| 14. Live Device And Field UAT | v1.2 | 0/1 | Blocked on local keys | - |
