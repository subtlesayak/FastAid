<div align="center">

# FastAid

Map-backed nearby aid discovery for roadside emergencies, breakdowns, and urgent public-safety moments.

![Android](https://img.shields.io/badge/Android-native-2E7D32?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-1565C0?style=for-the-badge&logo=openjdk&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-UI-C62828?style=for-the-badge&logo=materialdesign&logoColor=white)
![Google Maps](https://img.shields.io/badge/Google%20Maps-Places%20%2B%20Maps-9A4D00?style=for-the-badge&logo=googlemaps&logoColor=white)
![Safety Boundary](https://img.shields.io/badge/Safety-Prototype%20Only-212121?style=for-the-badge)

</div>

> FastAid is a prototype and controlled-pilot codebase. It is not an official emergency dispatch system. In real emergencies, call the official emergency number for your location.

## Overview

FastAid explores a simple question: when someone is under stress on the road, how quickly can they find the right kind of nearby help?

The project combines a native Android app, a browser prototype, and a pilot backend stub. The current product direction focuses on official emergency calling first, live nearby aid discovery second, and verified responder assignment only inside a controlled pilot.

## What FastAid Helps With

| Need | FastAid surface | Notes |
|---|---|---|
| Official emergency call | SOS tab | Surfaces country-aware emergency calling and India-specific official helplines. |
| Nearby public aid | Nearby tab | Uses Google Places data for hospitals, clinics, fuel, police, fire, repair, towing, pharmacies, toilets, rest stops, parking, and related categories. |
| Live navigation | Nearby + Map | Opens Google Maps navigation for selected places. |
| Local safety profile | Profile tab | Stores emergency contacts, blood group, medical notes, vehicle details, language, and offline preferences on device. |
| Controlled responder pilot | Backend stub | Models verified responder assignment separately from public Google Places results. |

## Project Surfaces

| Surface | Path | Purpose |
|---|---|---|
| Android app | [`android/`](android/) | Native Material 3 app with Maps SDK, Places SDK, emergency calls, local profile, and nearby aid categories. |
| Prototype | [`prototype/`](prototype/) | Browser prototype for service-flow exploration and UI iteration. |
| Backend | [`backend/`](backend/) | Node.js pilot backend for incidents, responder matching, readiness, audit events, and local notifications. |
| Pilot docs | [`docs/`](docs/) | Controlled pilot deployment and UAT guidance. |
| Verification scripts | [`scripts/`](scripts/) | Pilot artifact and source safety checks. |

## Core Principles

1. **Official help first**  
   Emergency numbers stay visible and callable before any product-specific flow.

2. **Public places are not responders**  
   Google Places results can help users discover nearby aid, but they are not treated as verified dispatch recipients.

3. **No invented aid**  
   If live Places data is unavailable, the app shows cached real results or an explicit recovery state. It does not fabricate nearby services.

4. **Privacy by default**  
   API keys, pilot data, admin credentials, and server secrets stay outside source control.

5. **Country-aware design**  
   Emergency calling and nearby aid discovery are designed to work beyond one city or country, with local configuration where required.

## Quick Start

### Backend and Prototype

```bash
npm install
npm run check
npm test
npm run verify:pilot
```

Run the local backend:

```bash
npm start
```

Open the browser prototype from [`prototype/index.html`](prototype/index.html), or use the backend-backed prototype after starting the server.

### Android

Open [`android/`](android/) in Android Studio, or run:

```powershell
cd android
.\gradlew.bat testDebugUnitTest assembleDebug
```

Debug APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

See [`android/README.md`](android/README.md) for Maps key setup and Android-specific behavior.

## Maps Key Boundary

FastAid uses separate key classes:

| Key type | Where it belongs | Restriction |
|---|---|---|
| Android display key | `android/local.properties` | Android package + SHA-1, Maps SDK for Android, Places API (New). |
| Backend server key | Deployment secret store | Server IP or service boundary, Places/Routes/Geocoding web services. |
| Web prototype key | Local `.env` or deployment env | HTTP referrer restrictions. |

Never commit `.env`, `.env.local`, `android/local.properties`, service-account JSON, backend data, or server-side Maps keys.

## Safety Status

FastAid is suitable for:

- Service design exploration
- Android UI prototyping
- Google Places integration testing
- Controlled pilot planning
- Verified-responder workflow simulation

FastAid is not yet suitable for:

- Public emergency dispatch
- Life-critical production use
- Multi-region responder operations
- Unsupervised automated emergency notifications

See [`docs/PILOT_DEPLOYMENT.md`](docs/PILOT_DEPLOYMENT.md) and [`docs/PILOT_UAT.md`](docs/PILOT_UAT.md) before running any controlled field test.

## Repository Quality Gates

```bash
npm run check
npm test
npm run verify:pilot
cd android
.\gradlew.bat testDebugUnitTest assembleDebug
```

The pilot artifact gate checks that secret files and pilot data are not tracked and scans source/build artifacts for configured server secrets.

## Roadmap Snapshot

| Phase | Focus | Status |
|---|---|---|
| MVP service model | Emergency calling, nearby categories, profile, offline-safe recovery | In progress |
| Live Places reliability | Google Places filtering, category quality scanner, cached real results | In progress |
| Controlled pilot backend | Incident lifecycle, responder matching, readiness, audit trail | Prototype |
| Verified responder network | Authenticated availability, assignment, accept/decline, notification delivery | Planned |
| Production hardening | Multi-instance storage, observability, real notification adapters, security review | Planned |

## Contributing

Contributions should preserve the safety boundary: do not make the app appear to dispatch public emergency services unless that flow uses verified, authenticated responders in a controlled environment.

Start with [`CONTRIBUTING.md`](CONTRIBUTING.md), then open a focused issue or pull request.

## License

No open-source license has been declared yet. Until a license is added, all rights are reserved by default.
