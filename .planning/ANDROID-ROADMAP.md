# FastAid Android Roadmap

GSD Core Version: 1.9.1

## Phase A1: Native App Shell

Status: Complete - debug APK builds successfully

Goal: Create the first installable Android app under `com.fastaid.app`.

Acceptance Criteria:

1. App launches from Android Studio.
2. User can see a FastAid emergency dashboard.
3. App can call the local FastAid backend from the emulator.
4. User can fetch nearby aid for accident, medical, breakdown, fuel, police, and fire scenarios.
5. Call and Go actions open the Android dialer and Google Maps navigation.
6. Current location uses a fresh device location fix when available, with a demo-location fallback.
7. SOS screen uses a focused, map-free emergency panel with country-aware helpline chips plus circular SOS, Locate, Share, and local-support actions; the dedicated Map tab owns live map context, while Nearby exposes the aid category grid and live place results.

## Phase A2: Native Maps Screen

Status: Complete - live map and POIs verified on device

Goal: Replace the list-first view with a Google Maps SDK screen showing user location, POIs, verified responders, and incident markers.

Acceptance Criteria:

1. Android Maps SDK key is read from `android/local.properties`. The placeholder is wired in Gradle and the manifest.
2. User location marker is visible after permission.
3. Nearby aid markers are grouped by category.
4. Tapping a marker opens aid details with Call and Go actions.

## Phase A3: Incident Creation Flow

Status: Complete - debug APK builds successfully

Goal: Build the one-tap emergency and structured report flow as native Android screens.

Acceptance Criteria:

1. One-tap emergency starts a visible countdown.
2. User can submit incident type, patient count, location, and optional notes.
3. Backend receives the incident and returns a lifecycle status.
4. User can cancel before dispatch during the countdown window.

## Phase A4: Responder Mode

Status: Complete - responder prototype verified on device

Goal: Add the responder-facing alert flow for verified ambulance, repair, fuel, police, and fire partners.

Acceptance Criteria:

1. Responder can see incoming incident cards.
2. Responder can accept or decline.
3. Accepted incidents show route and incident details.
4. User-facing status updates when aid is accepted.

## Phase A5: Offline And Low-Network Mode

Status: Complete - local queue and SMS fallback verified

Goal: Make the app useful when maps or data are unavailable.

Acceptance Criteria:

1. App shows the last known location.
2. User can call the configured emergency number without data.
3. SMS SOS payload can be prepared for low-connectivity mode.
4. Queued incident reports sync when network returns.

## Phase A6: Safety, Testing, And Pilot Readiness

Status: Complete - controlled demo build ready

Goal: Prepare the Android app for a controlled field demo.

Acceptance Criteria:

1. Permission-denied and location-off states are tested.
2. API failures show clear recovery options.
3. No unrestricted server API key is packaged in the app.
4. Pilot region configuration can change emergency number, categories, and units.

## Phase A1 Verification

Date: 2026-07-11

Result: Passed

Evidence:

1. `gradlew.bat assembleDebug` completed successfully.
2. Debug APK generated at `android/app/build/outputs/apk/debug/app-debug.apk`.
3. UI was revised after reference review to better match the FastAid MVP visual direction.
4. Existing backend smoke test previously passed with Google Places enabled.

## Phase A3 Verification

Date: 2026-07-11

Result: Passed

Evidence:

1. SOS ring starts a visible 5-second emergency countdown.
2. User can cancel before dispatch during the countdown window.
3. Incident report includes incident type, current/demo location, editable patient count, and optional notes.
4. `gradlew.bat assembleDebug` completed successfully after the A3 changes.
5. Debug APK regenerated at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Phase A1B Verification

Date: 2026-07-11

Result: Passed

Evidence:

1. Debug APK built successfully with `gradlew.bat assembleDebug`.
2. APK installed on connected device `R5CX91J0V8L` with `adb install -r`.
3. Backend was reachable from the physical device through `adb reverse tcp:4173 tcp:4173`.
4. Backend health reported `mapsProvider: google_places`.
5. Device UI showed the nearby-aid-first layout: map hero, country emergency call shortcut, `SHARE`, SOS prompt, two-row aid category grid, API incident card, and nearby places.
6. Nearby list showed real distances/ETAs and visible `CALL` / `GO` actions, including entries such as `Connaught Place Police Station` at `350 m`.
7. Device artifacts were captured at `fastaid-current.png` and `fastaid-ui.xml`.


## Phase A2 Verification

Date: 2026-07-13

Result: Passed

Evidence:

1. Google Maps SDK rendered on physical device `R5CX91J0V8L`.
2. Device-to-backend routing was restored with `adb reverse tcp:4173 tcp:4173`.
3. Backend health reported `mapsProvider: google_places`.
4. Device UI reported `15 nearby options found` and rendered live Google/verified place data.
5. Nearby cards exposed distance, ETA, open status, Call availability, and Go actions.

## Phase A4 Verification

Date: 2026-07-13

Result: Passed for prototype scope

Evidence:

1. Incidents tab rendered responder availability and incoming alert details.
2. Accept changed the user incident state to `EN ROUTE`.
3. Accepted state exposed `NAVIGATE` and `MARK ARRIVED` actions.
4. The UI explicitly states that the responder flow is a prototype and does not dispatch real responders.

## Phase A5 Verification

Date: 2026-07-13

Result: Passed for prototype scope

Evidence:

1. Last known coordinates persist in local preferences.
2. Failed incident submissions are queued locally and retried after API recovery.
3. Profile shows Offline and SMS SOS readiness with `PREPARE SMS` and the detected country emergency number.
4. SMS fallback opens the device composer with location, incident type, and people count.

## Phase A6 Verification

Date: 2026-07-13

Result: Passed for controlled demo

Evidence:

1. `:app:testDebugUnitTest` passed.
2. `:app:assembleDebug` passed and regenerated the debug APK.
3. Physical-device checks passed for live Places recovery, responder acceptance, and offline-profile controls.
4. Android backup is disabled for locally stored emergency data.
5. Maps SDK key remains supplied through ignored `android/local.properties`; the server-side Places key is not packaged in the APK.

Operational note:

The current physical-device demo uses a local backend. After reconnecting USB or restarting ADB, run `adb reverse tcp:4173 tcp:4173` before launching FastAid. A deployable pilot should replace this with an HTTPS backend URL.
