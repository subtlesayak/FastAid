---
phase: 10
plan: 1
status: complete
completed: 2026-07-13
requirements-completed: [PILOT-01, PILOT-02, PILOT-03]
---

# Summary: HTTPS Pilot Readiness

FastAid now has an explicit controlled-pilot deployment contract: Android receives its backend origin at build time, remote traffic requires HTTPS, backend liveness and readiness are separate, and a repeatable gate checks source and packaged artifacts for server-side secrets.

## Delivered

- Added pure local/pilot readiness evaluation and `GET /api/readiness` with `200`/`503` behavior.
- Added deployment checks for writable storage, HTTPS public URL, server Maps key, active administrator, and notification mode.
- Kept `/api/health` as independent process liveness.
- Added build-time Android `FASTAID_BACKEND_URL` injection through local properties or CI environment.
- Restricted Android cleartext traffic to loopback and emulator development hosts.
- Added safe root environment and Android local-property templates.
- Added source/APK/AAB server-secret and tracked-pilot-data verification.
- Added controlled single-process deployment guidance and a staged end-to-end field UAT.
- Added readiness unit and HTTP integration tests.
- Protected incident history/details and added bounded auth/incident abuse controls.
- Added bounded and range-validated account, incident, responder, and location inputs.
- Built the Android debug APK successfully with generated URL resources.

## Verification

- `npm run check`: passed
- `npm test`: 23 passed, 0 failed
- `npm run verify:pilot`: passed; 1 configured server secret checked
- `android\\gradlew.bat :app:assembleDebug`: build successful

## Decisions

- Local notifications are explicitly labeled simulated. Firebase selection remains unready until a real sender is injected.
- This milestone is fit for one controlled process only; public/life-critical deployment still requires managed persistence, durable jobs, remote notification delivery, operational monitoring, and agency agreements.
