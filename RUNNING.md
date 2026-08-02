# Running FastAid

## Local Backend

From the workspace root:

```bash
npm start
```

The local backend and API prototype run at `http://localhost:4173`.

Useful checks:

```txt
GET http://localhost:4173/api/health
GET http://localhost:4173/api/readiness
GET http://localhost:4173/api/nearby/aid?incidentType=Accident
GET http://localhost:4173/api/nearby/aid?incidentType=Breakdown
```

Local mode permits mock Places fallback and simulated responder notifications. Pilot mode uses the stricter readiness contract described in `docs/PILOT_DEPLOYMENT.md`.

## Pilot Operations Console

After configuring `FASTAID_ADMIN_EMAIL` and `FASTAID_ADMIN_PASSWORD`, open:

```txt
http://localhost:4173/operations.html
```

The console keeps its bearer session in memory, requires an administrator role for protected data, and supports responder verification/suspension, incident and duplicate review, audit history, notification attempts, readiness, and country configuration. Duplicate suggestions are review-only and never change incident state.

## Environment

Keep server configuration in root `.env` or deployment secrets:

```env
GOOGLE_MAPS_SERVER_KEY=your_server-restricted-key
FASTAID_ADMIN_EMAIL=pilot-admin@example.org
FASTAID_ADMIN_PASSWORD=use-a-secret-manager
FASTAID_SESSION_TTL_MINUTES=60
FASTAID_NOTIFICATION_PROVIDER=local
FASTAID_TRUST_PROXY=0
FASTAID_AUTH_RATE_LIMIT=20
FASTAID_INCIDENT_RATE_LIMIT=10
```

Use distinct Google keys:

- Server key: Places/Routes web services; never package it in Android.
- Android key: restrict by `com.fastaid.app` and signing SHA-1.
- Web key: restrict by allowed HTTP referrers.

## Android App

Copy the safe template values into `android/local.properties`:

```properties
MAPS_API_KEY=your-android-restricted-key
PLACES_API_KEY=your-android-restricted-key
FASTAID_BACKEND_URL=http://127.0.0.1:4173
```

The URL can also be supplied as the `FASTAID_BACKEND_URL` environment variable during a CI build. Gradle generates the Android string resource, so no Java or XML source edit is needed.

Maps and public nearby search run natively and do not need the backend. For incident/responder testing on a physical device using the local backend:

```bash
adb reverse tcp:4173 tcp:4173
```

Then build without installing:

```bat
cd android
gradlew.bat :app:assembleDebug
```

APK output:

```txt
android/app/build/outputs/apk/debug/app-debug.apk
```

Remote backend URLs must be HTTPS. Android cleartext traffic is limited to `127.0.0.1`, `localhost`, and emulator host `10.0.2.2`.

## Validation

```bash
npm run check
npm test
npm run verify:pilot
```

The artifact gate checks tracked secret/data files and scans source plus built APK/AAB artifacts for configured server-side secrets without printing their values.

## Static Prototype

The legacy static UI remains available at `prototype/index.html` and uses local mock data without a backend.
