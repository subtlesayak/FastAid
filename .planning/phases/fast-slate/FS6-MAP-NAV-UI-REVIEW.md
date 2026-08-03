# FS6 Map Navigation UI Review

Date: 2026-08-03
Scope: Android Map tab, selected-place sheet, floating emergency controls, and bottom navigation.
Review basis: GSD 6-pillar UI review plus Better UI polish heuristics.

## Evidence

- `fastaid-map-nav-review.png`: connected-device Map tab screenshot.
- `fastaid-map-nav-review.xml`: UIAutomator bounds and content-description evidence.

## Scorecard

| Pillar | Score | Notes |
| --- | ---: | --- |
| Copywriting | 3/4 | Labels are understandable, but the GPS chip is too verbose for the primary map surface and the SOS instructional copy appears behind/near the selected-place sheet. |
| Visuals | 3/4 | The map-first direction is strong, with recognizable floating controls and selected-place detail. The rail, card, and markers compete visually in the lower-right/lower-center region. |
| Color | 4/4 | Emergency red, location blue, and neutral surfaces are consistent with FastAid and remain memorable. |
| Typography | 3/4 | Place details are readable, but the GPS chip wraps into a large block and the selected-place card text density is high for an over-map element. |
| Spacing | 2/4 | Fixed rail/card margins work on the test device but create a crowded lower map area and obscure map labels, Google watermark space, and SOS helper copy. |
| Experience Design | 3/4 | Core actions are reachable and touch targets are large. The map still feels like an overlay stack rather than a clean navigation mode. |

Overall: 18/24. Needs changes before considering the Map tab polished.

## Findings

| Severity | Location | Before | After | Why |
| --- | --- | --- | --- | --- |
| HIGH | `android/app/src/main/java/com/fastaid/app/MainActivity.java:474` | Right action rail is fixed at bottom-end with `dp(124)` bottom margin, placing SOS/location/call/share over map POIs and labels. | Anchor the rail to the upper-right or middle-right safe zone on Map, or collapse to two primary controls with secondary actions in the place sheet. | The rail currently competes with markers and route context, which makes the live map harder to scan under stress. |
| HIGH | `android/app/src/main/java/com/fastaid/app/MainActivity.java:487` | Selected-place sheet is a wide bottom overlay with right margin only for the rail. | Convert it to a bottom sheet that sits above the bottom nav with a clear scrim/elevation, or use a compact marker card docked full-width outside Google watermark space. | The card obscures map content, partially collides with the SOS helper line, and visually fights the Google watermark and bottom navigation. |
| MEDIUM | `android/app/src/main/java/com/fastaid/app/MainActivity.java:498` | SOS helper text remains on the Map surface even when a place sheet is visible. | Hide the helper text on the dedicated Map tab unless SOS countdown is active, or move it into the SOS tab only. | Map mode should prioritize navigation and place inspection; persistent SOS education adds clutter. |
| MEDIUM | `android/app/src/main/java/com/fastaid/app/MainActivity.java:439` | GPS chip shows raw coordinates, accuracy, and age in one large wrapped chip. | Use a compact chip such as `Live GPS - 12 m - 7 min old`, with coordinates available in Profile/details. | Raw coordinates are useful but visually heavy; they steal attention from the map and selected aid. |
| MEDIUM | `android/app/src/main/java/com/fastaid/app/MainActivity.java:521` | Map tab resizes the same hero to 90% height after layout, but internal overlays keep fixed offsets. | Use a dedicated Map layout with safe-area constraints derived from bottom nav height and visible sheet state. | Responsive height without responsive overlay placement creates fragile alignment across devices. |
| LOW | `android/app/src/main/java/com/fastaid/app/MainActivity.java:1274` | Bottom navigation is usable, but its white bar visually cuts the map abruptly. | Add a subtle top divider/shadow and ensure map content ends behind a controlled inset, not abruptly under a flat white bar. | A stronger separation would make the map feel intentionally docked rather than clipped. |

## Bounds Check

- Action rail buttons: roughly `142 x 142 px`, sufficient for touch.
- Bottom nav items: roughly `185 x 153 px`, sufficient for touch.
- Selected-place Call/Go: roughly `116 x 116 px`, sufficient for touch.
- Main issue is visual occlusion and hierarchy, not minimum target size.

## Recommended Fix Order

1. Hide the SOS helper text on the Map tab except during an active countdown.
2. Make the selected-place sheet the only bottom overlay and reserve a fixed clear zone above bottom nav.
3. Move the action rail higher or reduce it to Location + Share, with SOS/Call remaining on the SOS tab and place card.
4. Compress the GPS chip and remove raw coordinates from default Map view.
5. Re-capture Map screenshot/XML on the connected device.

## Verdict

Needs changes. The Map tab is functionally usable, but the over-map controls are too crowded for FastAid's emergency-navigation goal.
