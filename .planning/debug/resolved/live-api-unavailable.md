---
status: resolved
trigger: "FastAid displayed Live API unavailable and no nearby Google Places results on the connected Android device."
created: 2026-07-13
updated: 2026-07-13
---

# Live API Unavailable

## Symptoms

- Expected: FastAid loads nearby live Google Places results on launch or refresh.
- Actual: Map SDK rendered, but the app showed `Live API unavailable`.
- Reproduction: Launch the physical-device build after USB/ADB reconnection.

## Current Focus

- hypothesis: resolved
- test: compare backend health, listening port, and ADB reverse state
- expecting: missing reverse mapping while backend and Google Places remain healthy
- next_action: none

## Evidence

- timestamp: 2026-07-13
  observation: Backend listened on port 4173 and `/api/health` returned `mapsProvider: google_places`.
- timestamp: 2026-07-13
  observation: `adb reverse --list` returned no mappings.
- timestamp: 2026-07-13
  observation: After `adb reverse tcp:4173 tcp:4173`, the app displayed `15 nearby options found`.

## Resolution

- root_cause: The physical-device app uses `http://127.0.0.1:4173`; the ADB reverse mapping had been lost after reconnecting the device.
- fix: Restored `adb reverse tcp:4173 tcp:4173` and relaunched FastAid.
- verification: On-device UI showed verified and public nearby aid with distance, ETA, open status, Call, and Go controls.
- files_changed: none required for the immediate runtime fix

## Follow-up

A deployable pilot must use an HTTPS backend URL. ADB reverse is only a local development bridge and must be re-applied after relevant device/ADB reconnects.
