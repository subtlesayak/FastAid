# FastAid Android

Native Android nearby-aid and emergency-assistance app.

## Package and stack

- Application ID: `com.fastaid.app`
- Minimum SDK: 26
- Language: Java
- UI: Material 3 (`material:1.14.0`)
- Maps: Google Maps SDK for Android
- Nearby data: Places SDK for Android (New) `5.3.0`

## Google key setup

An Android Maps key is client configuration, not a secret: it can be recovered from a debug APK. FastAid therefore keeps a development key in ignored `android/local.properties`, compiles it only into `debug`, and leaves `release` keyless until you deliberately configure a separate production key.

```properties
# Debug build only. Do not add quotes.
MAPS_API_KEY=your_debug_android_restricted_key

# Optional later, for a signed release only.
# RELEASE_MAPS_API_KEY=your_release_android_restricted_key
```

In Google Cloud, restrict the debug key to Android app package `com.fastaid.app` and the **debug signing SHA-1**. Restrict its API access to **Maps SDK for Android** and **Places API (New)**. Get the current signing fingerprints with:

```powershell
cd android
.\gradlew.bat signingReport
```

Use a distinct, separately restricted key with the release signing SHA-1 when preparing a production APK. Never package a server-side Maps key, Firebase Admin credentials, service-account JSON, or any other server secret. If the debug key is abused, revoke or rotate just that development key and check the Google Cloud API metrics and budget alerts.

`PLACES_API_KEY` is optional. When it is missing or blank, FastAid reuses the matching build's Maps key for the native Places SDK.

## Build and run

Open `android/` in Android Studio, or run:

```powershell
cd android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

APK output:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

The Android app does not require the local Node backend to show the map or fetch nearby public places. The backend remains optional for incident persistence and the verified-responder pilot flow.

For those backend-only features on a USB-connected phone:

```powershell
npm start
adb reverse tcp:4173 tcp:4173
```

## Current behavior

- Requests the device location before searching
- Uses a recent location immediately and asks for a fresh GPS fix
- Queries Google Places natively for hospitals, police, fire, repair, fuel, pharmacy, doctors, tyre shops, parts stores, and EV charging
- Shows real distance, estimated travel time, available phone number, and opening status
- Sorts known-open places first, then callable places, then distance
- Opens the dialer from Call and Google Maps navigation from Go
- Keeps Call 112, current location, SOS, and Share together on the map
- Provides All, Open now, and Callable filters
- Provides editable emergency contacts, blood group, medical, vehicle, language, and settings in Profile
- Keeps the bottom navigation above gesture and three-button system navigation

## Failure behavior

FastAid never inserts invented POIs. If a live refresh fails, it shows previously cached Google Places results for up to seven days and labels them as saved data. If no real cache exists, it shows Call 112, Use GPS, and Retry.

If location permission is denied, the app uses only the last location actually saved on that device and labels it accordingly. It does not substitute a demo city.

## Keyless development

The app builds and the non-map flows can be tested with empty key fields. In that state, Google map tiles and native Places remain disabled, and the emergency-safe recovery state is shown. Add the key before live Places UAT.

`assembleDebug` reads `MAPS_API_KEY`; `assembleRelease` reads only `RELEASE_MAPS_API_KEY`. This avoids accidentally shipping a development key in a release APK.

## Safety boundary

Public Google Places are discovery results, not dispatched responders. Only authenticated, verified FastAid responders may enter the responder assignment workflow. Do not present this prototype as official emergency dispatch.
