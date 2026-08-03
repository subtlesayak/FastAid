# FS6 Profile UI Review

Date: 2026-08-03
Scope: Android Profile tab on the connected development device.
Review basis: GSD UI review pillars plus Better UI heuristics for emergency-service clarity, tap safety, and bottom-navigation ergonomics.

## Evidence

- `fastaid-profile-audit-fs6-fixed.png` / `.xml`: Profile top after accessibility and bottom-padding fixes.
- `fastaid-profile-audit-fs6-mid.png` / `.xml`: Mid-scroll Profile sections covering medical, vehicle, country, language, and readiness controls.
- `fastaid-profile-audit-fs6-bottom.png` / `.xml`: Lower Profile sections covering map data, offline/SMS SOS, and preferences.
- `fastaid-profile-audit-fs6-end.png` / `.xml`: End-of-scroll verification that emergency actions and privacy note clear the bottom navigation.
- `fastaid-profile-top.png` / `.xml`: Current Profile top-state review after country-language and Broseph changes.
- `fastaid-profile-bottom.png` / `.xml`: Country/language and app-readiness review on the connected device.
- `fastaid-profile-language-fixed-bottom.png` / `.xml`: India-gated language picker scrolled to Odia and Malayalam with Save/Cancel clear of the options.
- `fastaid-profile-contacts-fixed.png` / `.xml`: Emergency contacts dialog with primary/secondary action stack.
- `fastaid-country-helplines-clean.png` / `.xml`: SOS panel showing country-aware India helplines rendered from the shared emergency registry.
- `fastaid-profile-validation.png` / `.xml`: Profile top after local-only safety profile validation helpers were added.
- `fastaid-phone-editor-validation.png` / `.xml`: Phone editor helper text and device-local storage disclosure.
- `fastaid-profile-bottom-padding.png` / `.xml`: Lower Profile review after increased bottom scroll padding.
- `fastaid-profile-fs6-final-bottom.png` / `.xml`: Final installed-build Profile end state showing emergency actions and privacy note clear of both app and Android navigation.

## Scorecard

| Pillar | Score | Notes |
| --- | ---: | --- |
| Copywriting | 4/4 | Labels are direct and emergency-oriented: contacts, blood group, vehicle, current location, SOS, profile storage, and privacy all read clearly. |
| Visuals | 4/4 | Material-style cards, large controls, readable status chips, and the corrected dialog action stack fit the emergency use case. |
| Color | 4/4 | Red remains reserved for emergency emphasis, blue supports responder/service actions, and neutral cards keep the profile calm. |
| Typography | 4/4 | Hierarchy is readable across hero, section headers, labels, values, and privacy copy. Text wraps safely in the captured views. |
| Spacing | 4/4 | Dialog spacing is fixed for regional language scrolling, and Profile now carries extra bottom padding so lower emergency actions can clear the fixed app and Android navigation areas. |
| Experience Design | 4/4 | Profile works as a safety handoff surface. Contacts, blood, vehicle, country emergency number, language, readiness, and emergency actions have clear entry points. |

Overall: 24/24. Approved for FS6.

## Fixes Applied

- Added extra bottom scroll padding so Profile content clears the app bottom navigation and Android navigation area at the end of the page.
- Replaced generic emergency handoff chip TalkBack text with action-specific descriptions for contacts, blood group, vehicle, and current location.
- Added a content description to the Profile completeness indicator.
- Rebuilt, tested, and installed the debug APK after the Profile fixes.
- Gated regional language choices by detected country: India shows English plus Hindi, Bengali, Telugu, Marathi, Tamil, Gujarati, Urdu, Kannada, Odia, and Malayalam; other countries show English until country packs exist.
- Replaced the clipped system language list with a bounded scrolling Material picker so Malayalam remains reachable and Save/Cancel remain separate.
- Reworked the Emergency contacts dialog into a clearer action stack: primary Pick contact, secondary Edit manually, quiet Cancel.
- Added a Bengaluru-only Broseph FastAid tool as a distinct circular "B" shortcut instead of a generic call icon.
- Replaced hardcoded India-only helpline rendering with a country-aware emergency registry that powers the SOS panel and primary emergency-number resolver.
- Added local validation helpers for profile name, phone, emergency contacts, medical notes, vehicle make/model, registration, and insurance fields.
- Added profile editor helper text so sensitive handoff values are clearly framed as device-local information.
- Increased Profile bottom scroll padding again so end-of-page actions have more breathing room above fixed navigation.

## Remaining Improvements

- Consider reducing the repeated oversized row edit icons to subtler trailing affordances once every profile field has row-level edit behavior.
- Add country-specific language packs beyond India before exposing regional language choices in non-India locations.
- Add source-backed country helpline audits before claiming exhaustive coverage for every supported country.
- Add instrumentation coverage for invalid profile field saves; the manual ADB text-entry attempt did not reliably land text in the dialog field.

## Verdict

The Profile tab now supports the FastAid nearby-aid MVP as a practical emergency handoff page: it is readable, scrollable, country-aware, dialog-safe, and accessible enough for the current Slate. Remaining work is polish, not a blocker for the fast nearby-aid MVP.
