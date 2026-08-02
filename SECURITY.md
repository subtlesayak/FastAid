# Security Policy

FastAid handles safety-adjacent flows and location-derived data. Treat security and privacy issues as high priority, even while the project is a prototype.

## Do Not Commit

- `.env` or `.env.local`
- `android/local.properties`
- Backend server keys
- Firebase or service-account credentials
- Admin passwords
- Pilot data stores
- Personal responder or participant data
- Built APK/AAB artifacts containing server secrets

## Supported Security Boundary

| Area | Current boundary |
|---|---|
| Android Maps key | Client display key only, restricted by package and signing certificate. |
| Server Maps key | Backend-only secret, never packaged into the app. |
| Public Places data | Discovery only, not verified responder dispatch. |
| Pilot notifications | Local/simulated unless a real provider is explicitly configured. |
| Profile data | Stored locally on device in the current prototype. |

## Reporting

Open a private security advisory if available, or create a minimal issue that does not include secrets, exploit payloads, personal data, or live emergency details.

Include:

- Affected surface: Android, backend, prototype, docs, or CI
- Impact summary
- Reproduction steps with dummy data
- Suggested mitigation, if known

## Verification

Before publishing or sharing artifacts, run:

```bash
npm run verify:pilot
```

For Android release work, use a separate release key and verify that no server-side key is packaged.
