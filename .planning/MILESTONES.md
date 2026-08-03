# FastAid Milestones

## v1.0 Android Nearby-Aid MVP

**Shipped:** 2026-07-13

Delivered:

- Native Android app under `com.fastaid.app`
- Live Google Maps and nearby Google Places data
- Distance, ETA, opening status, Call, and Go actions
- SOS countdown and incident creation
- Responder acceptance prototype with en-route and arrival states
- Safety profile, local queue, SMS SOS, and Call 112 fallback
- Physical-device verification and installable debug APK

Evidence is retained in `.planning/ANDROID-ROADMAP.md` and `GSD_PHASE_STATUS.md`.

## v1.1 Pilot Backend Foundations

**Completed:** 2026-07-13

Delivered:

- Persistent incident, alert, responder, user, session, audit, and delivery-attempt state
- Salted credentials, expiring sessions, roles, ownership, and append-only audit APIs
- Verified responder ranking, single-active assignment, retries, decline/timeout rollover, and forward-only lifecycle
- Android build-time backend URL, remote HTTPS policy, liveness/readiness, and source/APK secret gate
- Controlled-pilot deployment and field-UAT documentation

Audit: 19/19 requirements satisfied with documented single-process pilot technical debt. See `.planning/v1.1-MILESTONE-AUDIT.md`.

## v1.2 Pilot Operations

**Started:** 2026-07-16

Planned outcomes:

- Authenticated responder verification, suspension, incident review, and audit operations UI
- Real responder and emergency-contact notification delivery
- Managed persistence and durable alert jobs
- Monitoring, backup/restore evidence, live-key device UAT, and staged field exercise

Status: Phase 11 complete and verified on 2026-07-16. Phase 12 remote notification delivery is next.

## The Fast Slate

**Started:** 2026-08-03

Purpose:

- Repair the Android app around fast nearby aid before expanding responder-side services.
- Tighten emergency surface trust, live Places result quality, map context, profile usefulness, accessibility, localization, and physical-device evidence.
- Keep public Google Places POIs discovery-only; only verified responders can enter dispatch in later pilot phases.

Status: FS1 and FS2 are underway. Full phase plan: `.planning/phases/fast-slate/FAST-SLATE-PLAN.md`.
