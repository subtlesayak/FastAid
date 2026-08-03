# Phase 11 UI Review: FastAid Android App Surface

Date: 2026-07-27
Reviewer: Codex
Scope: Installed Android APK on connected device, FastAid user app only. Responder-side service is out of scope per current product boundary.

## Screenshot Log

| Screenshot | Screen | Notes |
| --- | --- | --- |
| `fastaid-audit-01-current.png` | Nearby Aid, ATM selected | Live Google Places works and returns 20 ATM options, but result-card icons fall back to a green vehicle/emergency glyph instead of an ATM glyph. |
| `fastaid-audit-02-sos.png` | Nearby Aid, expanded categories | Core and extended categories render as native emoji, producing inconsistent style, weight, color, and platform-dependent appearance. |
| `fastaid-audit-03-map.png` | Profile tab | The captured state shows the profile page, confirming bottom navigation can reach Profile and the safety profile card is present. |
| `fastaid-audit-04-incidents.png` | Blood group dialog | Profile edit flow opened correctly, but modal visual language is more default Material than FastAid-branded. |
| `fastaid-audit-05-profile.png` | Blood group transition frame | The modal was captured during transition, showing temporary overlap. Not a steady-state defect unless seen during normal use. |
| `fastaid-audit-06-nearby-clean.png` | Nearby Aid, clean state | Confirms the same icon and category consistency issues without modal interference. |

## Score Summary

Overall: 15/24

| Pillar | Score | Assessment |
| --- | ---: | --- |
| Copywriting | 3/4 | Labels are short and emergency-oriented. Status text is useful, but some category labels need domain wording such as "Tyre repair" instead of "Tyres". |
| Visuals | 2/4 | App structure is recognizable and Material 3 base is present, but emoji category icons break the professional emergency-tool feel. |
| Color | 3/4 | Red, blue, green, and orange are memorable and map well to emergency/support/utility, but result color fallback misrepresents categories. |
| Typography | 3/4 | Text is readable and large enough on most surfaces. Result names can become dense when long place names wrap beside large action buttons. |
| Spacing | 2/4 | Bottom navigation is now visible above system navigation, but the expanded category grid consumes too much vertical space before results. |
| Experience Design | 2/4 | Live Places data works in the observed ATM state, but category responses and visual confirmation are not trustworthy enough yet for a fast aid app. |

## Findings

### P0 - Result category icons are incorrect

Evidence:
- Screenshot: `fastaid-audit-01-current.png`
- Source: `android/app/src/main/java/com/fastaid/app/MainActivity.java`, `placeIconResource(...)` around line 3653

ATM results show a green vehicle/emergency icon. The root cause is that `placeIconResource(...)`, `placeColor(...)`, and map marker `placeIcon(...)` do not include mappings for `atm`, `food`, `lodging`, `car_wash`, `auto_parts`, and related extended categories. They fall through to generic accident or green defaults.

Fix:
- Add uniform Material vector drawables for ATM, restaurant/food, lodging, car wash, towing, battery, and workshop.
- Extend `placeIconResource(...)`, `placeColor(...)`, and `placeIcon(...)` so every Places category has a correct visual identity.

### P0 - Category tile icons are not Material 3 consistent

Evidence:
- Screenshot: `fastaid-audit-02-sos.png`
- Source: `android/app/src/main/java/com/fastaid/app/MainActivity.java`, `emojiCategoryChip(...)` around line 778

Native emojis have mismatched size, lighting, shadows, perspective, and color. This weakens FastAid's "calm, operational emergency tool" direction from `.planning/UI-SPEC.md`.

Fix:
- Replace all `emojiCategoryChip(...)` calls with the existing `categoryChip(...)` pattern.
- Use white vector Material-style icons on consistent colored circular markers.
- Remove the emoji rendering path after replacement.

### P1 - Expanded category grid delays the result list

Evidence:
- Screenshot: `fastaid-audit-02-sos.png`

Expanded Nearby Aid shows six full rows of category tiles before filters and results. On a phone screen, this pushes the actionable place list below the fold and makes the user scroll during a stressful task.

Fix:
- Use a compact 3-column or horizontally grouped category grid, or pin the selected category and collapse secondary categories after selection.
- Keep the first result card visible whenever live results are loaded.

### P1 - Category quality scanner needs full extended-category coverage

Evidence:
- Source: `android/app/src/main/java/com/fastaid/app/GooglePlacesRepository.java`, `normalizeCategory(...)` around line 260
- Source: `android/app/src/main/java/com/fastaid/app/ServiceQualityScanner.java`, `isRelevant(...)` around line 40

The scanner has good safety logic for accident, fire, and police, but the new categories need tighter tests. `ebike_charging_station` can fall through as a raw type unless normalized before the broader electric check.

Fix:
- Normalize `ebike_charging_station` to `ev` or a dedicated `ebike` category consistently.
- Add tests for ATM, food, lodging, car wash, towing, battery, and workshop.
- Keep accident results restricted to medical/police/fire, not car accessories or unrelated hospitals.

### P1 - Profile dialogs feel less branded than the page

Evidence:
- Screenshots: `fastaid-audit-04-incidents.png`, `fastaid-audit-05-profile.png`

The blood group selector works, but the default dialog surface is a pale system-purple Material style while the rest of FastAid uses red/blue/green operational color memory.

Fix:
- Use a FastAid-themed Material 3 bottom sheet or styled alert with the same surface, shape, and action styling as the profile cards.
- Keep blood group radio rows large and screen-reader friendly.

### P2 - Status text can be more actionable

Evidence:
- Screenshots: `fastaid-audit-01-current.png`, `fastaid-audit-02-sos.png`

"Live Google Places - 20 nearby options" is helpful. "Finding nearby aid..." can feel indefinite if the request is slow.

Fix:
- Show category-specific progress: "Finding car wash nearby..." or "Showing 20 ATMs nearby".
- If Places fails, show the exact recovery action: "Check API key, billing, or network" only in debug; user-facing production copy should say "Live places unavailable. Try again or call emergency services."

## Better UI Improvements

### Surfaces and radius

| Before | After |
| --- | --- |
| Category tiles use card borders plus emoji visuals with no shared icon geometry. | Use one icon container size, one corner radius, and one selected state treatment across all categories. |
| Result cards use generic icon fallback, making category identity unreliable. | Make category identity explicit with category-specific glyph, color, badge, and optional service-quality label. |

### Hit areas

| Before | After |
| --- | --- |
| Call and Go buttons are large enough, but categories occupy large cards even when the user needs results. | Preserve 44dp minimum targets while reducing tile height in expanded mode so results stay visible. |

### Optical alignment

| Before | After |
| --- | --- |
| Emoji icons have platform-defined optical centers and uneven internal padding. | Vector icons should be manually centered inside the same circular marker size. |

## Recommended Next Patch

1. Replace Nearby category emoji tiles with vector Material-style icons.
2. Add missing vector drawables for extended categories.
3. Fix result-card and map-marker category mapping.
4. Add repository/scanner tests for every category query and normalized response.
5. Rebuild, install, and screenshot SOS, Map, Nearby, Incidents, and Profile again.

