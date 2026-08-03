# Phase 10 Context: HTTPS Pilot Readiness

## Boundary

Phase 10 prepares FastAid for a controlled single-process pilot. It does not claim production emergency-service reliability, multi-instance safety, official dispatch integration, or remote Firebase delivery.

## Decisions

- Keep `/api/health` as liveness and add `/api/readiness` for configuration/dependency gating.
- Require HTTPS public URL, server Maps key, active administrator, and writable storage in `pilot` mode.
- Report local notifications as ready but simulated; report Firebase as unready until a real sender is injected.
- Inject the Android backend URL from `android/local.properties` or `FASTAID_BACKEND_URL` at build time.
- Allow Android cleartext only for loopback/emulator development domains; remote endpoints must use HTTPS.
- Add an artifact gate that compares configured server secrets against source and built APK/AAB bytes without printing secret values.
- Document a staged, non-emergency field UAT and make the single-process limitation prominent.

## Deployment Contract

- Node serves private HTTP behind an HTTPS reverse proxy or managed platform.
- Pilot data lives outside the repository at `FASTAID_DATA_FILE`.
- Deployment secrets come from the platform secret store.
- A production migration must replace JSON persistence, in-process timeout sweeps, and simulated notification delivery.
