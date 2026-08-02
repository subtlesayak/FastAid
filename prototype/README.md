# FastAid MVP Prototype

This is a static browser prototype for the FastAid app concept.

Open `index.html` directly in a browser. It currently uses mock maps and nearby-aid data so it can run without a Google Maps key.

## What It Demonstrates

1. Map-first emergency app shell
2. One-tap SOS flow
3. Accident, breakdown, fuel, medical, police, fire, repair, pharmacy, EV, and towing categories
4. Nearby aid list with verified responder versus public place labels
5. Responder alert and accept/decline flow
6. Offline/SMS fallback state
7. Provider boundary for replacing mock data with Google Places/Routes

## Maps Key Placement

Use root `.env.example` as the template.

Server-only key:

```env
GOOGLE_MAPS_SERVER_KEY=
```

Client display keys:

```env
GOOGLE_MAPS_ANDROID_KEY=
GOOGLE_MAPS_IOS_KEY=
GOOGLE_MAPS_WEB_KEY=
```

Do not put the server key inside browser JavaScript. In production, the app should call your backend, and the backend should call Google Places, Routes, and Geocoding.
