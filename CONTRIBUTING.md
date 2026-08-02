# Contributing to FastAid

FastAid is a safety-adjacent prototype, so contributions should be small, reviewable, and explicit about what is real versus simulated.

## Ground Rules

- Do not commit secrets, keys, tokens, pilot data, private responder details, or device-specific local files.
- Do not present Google Places results as verified dispatchable responders.
- Do not add copy that claims FastAid replaces official emergency services.
- Do keep failure states honest: show unavailable, cached, simulated, or local-only states clearly.
- Do keep public documentation neutral and free of personal identifiers.

## Local Checks

Run the relevant checks before opening a pull request:

```bash
npm run check
npm test
npm run verify:pilot
```

For Android changes:

```powershell
cd android
.\gradlew.bat testDebugUnitTest assembleDebug
```

## Pull Request Shape

Good pull requests include:

- A short summary of the user-facing change
- The safety boundary affected, if any
- Screenshots for UI changes
- Test commands and results
- Notes on any remaining limitation

## UI Guidelines

- Keep emergency actions visually dominant and easy to scan.
- Use Material 3 components and existing FastAid color roles.
- Keep tap targets large enough for high-stress use.
- Prefer direct labels such as `Call 112`, `Use GPS`, `Go`, and `Share`.
- Avoid decorative UI that slows down urgent decision-making.

## Backend Guidelines

- Keep server-only Maps keys and provider credentials outside source.
- Maintain audit events for protected mutations.
- Keep readiness checks strict when pilot mode is enabled.
- Use explicit source labels such as `verified_responder`, `public_place`, `cache`, and `simulated`.
