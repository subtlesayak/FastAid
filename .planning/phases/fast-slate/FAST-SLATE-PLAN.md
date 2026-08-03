# The Fast Slate Plan

GSD Core Version: 1.9.1

## Milestone Intent

Make the Android app feel like a fast nearby-aid service before expanding responder-side systems. The app must clearly separate official emergency calling, live Google Places discovery, cached/offline fallback, and verified-responder pilot claims.

## Phase FS1: Emergency Surface Trust

Goal: Make the first SOS screen safe, fast, and free of stale demo identity.

Acceptance:
- Official numbers are the first primary action cluster.
- SOS, Locate, Share, Voice, and manual location remain reachable without map clutter.
- Demo/default profile data does not count as user-complete emergency data.
- Existing test-device installs with only stale demo identity are reset to an unset profile state.

## Phase FS2: Nearby Aid Quality Slate

Goal: Make category results trustworthy enough for stressful decisions.

Acceptance:
- Results are grouped into best matches and check-before-using results.
- Accident prioritizes actual emergency/medical aid over commerce noise.
- Police and fire categories do not promote generic POIs above stations/outposts/services.
- Call buttons clearly express unavailable phone data; Go remains available when coordinates exist.
- The result scanner explains why a place is ranked.

## Phase FS3: Map-First Live Context

Goal: Keep Map as a mostly full-screen, live Google Maps surface.

Acceptance:
- Map uses around 90% of the app viewport on the Map tab.
- Emergency controls do not cover key map labels or bottom navigation.
- Selecting a marker exposes place name, category, distance, quality, Call, and Go.
- Cached/static fallback is visibly distinct from live Google Maps.

## Phase FS4: Safety Profile Material 3

Goal: Make Profile useful as an emergency handoff record.

Acceptance:
- Sections are Identity, Emergency Contacts, Medical, Vehicle, Preferences, and Privacy.
- Emergency contacts support picker/import, priority order, and visible count.
- Blood group, language, vehicle type, registration, insurance, and notes have clear edit flows.
- Profile completeness counts only real user-entered data.

## Phase FS5: Accessibility And Localization Sweep

Goal: Make the app usable under stress, with assistive tech, and in Indian languages.

Acceptance:
- All icon-only actions have clear labels and roles.
- Custom category tiles are announced as selectable buttons with selected state.
- Text remains usable at large font scale.
- Supported languages through Malayalam change visible app copy where translations exist.
- Emergency number behavior is country-aware and test-covered.

## Phase FS6: Connected-Device Verification

Goal: Prove every public-user path on the attached device.

Acceptance:
- Device screenshots and UI XML are captured for SOS, Map, Nearby, Incidents, Profile, dialogs, and category results.
- Category taps refresh live/cached results.
- Call and Go intents launch correctly.
- Missing API, denied location, stale GPS, and offline modes fail with safe instructions.
