# FastAid Controlled Pilot Deployment

FastAid v1.1 is ready for a **single-process controlled pilot**, not a public emergency-service launch. The JSON store, in-process alert sweep, and simulated local notification adapter must be replaced before multi-instance or life-critical production use.

## Backend Configuration

Set deployment secrets outside source control:

```env
FASTAID_DEPLOYMENT_MODE=pilot
FASTAID_PUBLIC_BASE_URL=https://api.example.org
FASTAID_DATA_FILE=/var/lib/fastaid/pilot-data.json
FASTAID_ADMIN_EMAIL=pilot-admin@example.org
FASTAID_ADMIN_PASSWORD=use-a-secret-manager
FASTAID_SESSION_TTL_MINUTES=60
GOOGLE_MAPS_SERVER_KEY=server-restricted-key
FASTAID_NOTIFICATION_PROVIDER=local
```

`local` notifications are simulated and explicitly reported as such by readiness. Selecting `firebase` keeps readiness false until a real sender is injected into the adapter.

Run Node behind an HTTPS reverse proxy or managed platform. TLS terminates at that boundary; the FastAid process serves HTTP on its private port.

## Readiness

- `GET /api/health` answers process liveness and always returns `200` while the process can serve requests.
- `GET /api/readiness` evaluates storage, HTTPS public URL, Maps configuration, administrator bootstrap, and notification mode. Pilot misconfiguration returns `503` with non-secret check details.

Do not route pilot traffic until readiness returns `200` and the warning list matches the intended notification mode.

## Android Configuration

Put build-local values in `android/local.properties`:

```properties
MAPS_API_KEY=android-package-and-sha1-restricted-key
PLACES_API_KEY=android-package-and-sha1-restricted-key
FASTAID_BACKEND_URL=https://api.example.org
```

The same backend URL can be supplied in CI as the `FASTAID_BACKEND_URL` environment variable. No Java or XML source edit is required. Android permits cleartext only for emulator/loopback development hosts; remote pilot endpoints must use HTTPS.

## Artifact Gate

Run:

```bash
npm run verify:pilot
```

The gate checks that `.env`, `android/local.properties`, and pilot data are not tracked, then searches source and any built APK/AAB files for configured server secrets. An Android-restricted Maps display key is expected in the APK; a server key is not.
