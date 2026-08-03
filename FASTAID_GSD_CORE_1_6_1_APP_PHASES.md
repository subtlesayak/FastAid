# FastAid App Development Plan

GSD Core Version: 1.9.1

Project: FastAid

Concept: A country-agnostic emergency assistance app that uses real maps and places data to surface nearby aid options such as hospitals, police stations, fire stations, petrol pumps, vehicle repair centers, tire shops, pharmacies, EV charging stations, rest stops, and verified first responders.

## Product Thesis

FastAid should not depend on one country's emergency infrastructure. The app can use map and POI providers to identify nearby emergency-relevant places in most regions, while verified responders, dispatch workflows, and government emergency integrations remain configurable per country.

Google Maps Places API can support discovery of nearby places such as `hospital`, `general_hospital`, `medical_center`, `medical_clinic`, `pharmacy`, `police`, `fire_station`, `gas_station`, `car_repair`, `tire_shop`, `electric_vehicle_charging_station`, `parking`, and `rest_stop`.

Important constraint: Places/POI APIs locate places; they do not dispatch help. FastAid needs its own verified responder network or an integration with official emergency systems for actual notifications.

## Core User Groups

1. Victim or bystander
2. Ambulance driver or medical responder
3. Police, fire, or official field responder
4. Roadside assistance provider
5. Hospital or clinic operator
6. Repair center, tire shop, petrol pump, or towing partner
7. Dispatch/admin operator

## Main Service Modes

1. Medical emergency
2. Road accident
3. Vehicle breakdown
4. Fuel assistance
5. Fire/police emergency
6. Unknown emergency / SOS
7. Offline low-network emergency

## Technical Direction

Maps and POI layer:

- Use Maps SDK for map display.
- Use Places API Nearby Search for emergency-relevant POIs.
- Use Place Details for phone number, address, open status, and website where available.
- Use Routes API for ETA and distance.
- Cache selected area data for low-connectivity use.
- Keep a provider abstraction so Google Maps can later be swapped or combined with Mapbox, OpenStreetMap/Overpass, local government datasets, or partner APIs.

Responder layer:

- Maintain FastAid's own verified responder/partner database.
- Track responder availability, current location, service type, and acceptance status.
- Notify through push notification, SMS, automated call, or dispatch dashboard.
- Let responders accept, decline, update ETA, and mark resolved.
- Keep official emergency number calling available in every region.

Safety layer:

- Always show a direct emergency-call fallback.
- Distinguish "nearby places found by maps" from "verified responders available through FastAid."
- Avoid promising official dispatch unless a real government/agency integration exists.
- Store audit logs for emergency requests, notifications, accepts, declines, and handoffs.

## Milestones

### Milestone 0: Project Foundation

Goal: Define FastAid as a global emergency assistance platform, not only an India-specific accident app.

Deliverables:

- Project charter
- Stakeholder map
- Safety and liability assumptions
- Country-agnostic service model
- Initial API feasibility notes
- GSD workspace baseline

Acceptance Criteria:

- Product scope separates POI discovery from responder dispatch.
- Emergency categories include medical, accident, breakdown, fuel, police, fire, and offline SOS.
- Country-specific features are behind configuration, not hardcoded.

### Milestone 1: Research and Requirements

Goal: Convert the capstone research into build-ready product requirements.

Deliverables:

- User personas for victim, bystander, responder, and roadside partner
- Problem statements and How Might We questions
- Emergency scenario matrix
- Functional requirements
- Non-functional requirements
- Risk register

Acceptance Criteria:

- Requirements include fast reporting, minimal input, live map, voice support, duplicate dispatch avoidance, and offline fallback.
- Requirements explicitly include repair centers, petrol pumps, tire shops, pharmacies, hospitals, police, and fire stations.
- Each requirement has a priority: MVP, v1, or future.

### Milestone 2: Maps and POI Feasibility Spike

Goal: Prove that FastAid can locate useful aid options from real-world maps data.

Deliverables:

- Google Maps Platform setup
- Nearby Search prototype
- POI category mapping
- Route and ETA prototype
- Place detail lookup prototype
- Cost, quota, and latency estimate

Acceptance Criteria:

- App can search around a user location for hospitals, police, fire stations, gas stations, car repair, tire shops, pharmacies, and EV charging.
- Results can be ranked by distance, estimated travel time, open status, and category relevance.
- The prototype clearly labels unverified map results versus verified FastAid partners.

### Milestone 3: Service Architecture

Goal: Design the backend and data model that connects users, POIs, responders, and alerts.

Deliverables:

- System architecture diagram
- API contract
- Database schema
- Incident lifecycle model
- Responder availability model
- Notification routing design
- Audit logging design

Acceptance Criteria:

- Incidents have states: created, verifying, notified, accepted, en route, arrived, resolved, cancelled.
- Responders have states: offline, available, notified, accepted, busy, unavailable.
- Maps POIs and verified responders are stored separately.
- Dispatch can fall back from app notification to SMS/call.

### Milestone 4: UX and Information Architecture

Goal: Finalize the app structure for stressful, fast-use situations.

Deliverables:

- Information architecture
- Navigation model
- Role-based user flows
- Emergency report flow
- Nearby aid flow
- Responder acceptance flow
- Offline SOS flow

Acceptance Criteria:

- Primary user can request help in one tap.
- Manual reporting takes no more than a few required fields.
- The map remains the main interface.
- UI separates "call emergency number", "notify FastAid responders", and "find nearby places."

### Milestone 5: UI Design System

Goal: Build a consistent, accessible visual system for mobile UI.

Deliverables:

- Color palette
- Typography scale
- Icon set
- Emergency state colors
- Button styles
- Map marker system
- Form components
- Bottom navigation
- Alert cards
- Responder cards

Acceptance Criteria:

- Emergency actions are visually dominant.
- Color coding is consistent across medical, repair, fuel, police, fire, and general SOS.
- UI is readable in bright outdoor conditions.
- Controls are usable under stress and one-handed operation.
- Voice and accessibility controls are part of the system, not add-ons.

### Milestone 6: Low-Fidelity UI Prototype

Goal: Validate layout and flow before visual polish.

Deliverables:

- Login/signup wireframe
- Home map wireframe
- One-tap SOS wireframe
- Accident report wireframe
- Breakdown/fuel request wireframe
- Nearby aid list wireframe
- Live tracking wireframe
- Responder alert wireframe
- Profile/settings wireframe

Acceptance Criteria:

- All MVP flows are represented.
- User can navigate from SOS to incident tracking.
- Responder can accept or decline a request.
- Nearby POIs can be filtered by category.

### Milestone 7: High-Fidelity UI Prototype

Goal: Convert the validated wireframes into presentation-ready and implementation-ready screens.

Deliverables:

- High-fidelity mobile screens
- Clickable prototype
- Interaction notes
- Empty, loading, error, and offline states
- Map marker visuals
- Responder and POI detail sheets

Acceptance Criteria:

- Prototype includes user and responder perspectives.
- Screens cover medical emergency, accident, vehicle repair, fuel assistance, and nearby aid.
- The UI clearly marks verified responders versus map-discovered places.
- Offline mode has visible state and fallback actions.

### Milestone 8: MVP Mobile App

Goal: Build the first working app shell.

Deliverables:

- Mobile app project setup
- Authentication
- Location permission flow
- Map screen
- Category filters
- Nearby aid search
- Incident creation form
- Profile and emergency contacts
- Settings

Acceptance Criteria:

- User can log in, view map, search nearby aid, and create a test incident.
- User can filter POIs by medical, police, fire, fuel, repair, pharmacy, and roadside support.
- App handles denied location permission gracefully.
- App has basic offline fallback UI.

### Milestone 9: Backend and Responder Network

Goal: Build the service layer required for real notifications.

Deliverables:

- Backend API
- User and responder accounts
- Partner onboarding
- Responder verification workflow
- Incident matching service
- Push/SMS notification service
- Dispatch/admin dashboard MVP

Acceptance Criteria:

- Verified responder can receive an incident notification.
- Responder can accept or decline.
- User can see accepted responder and ETA.
- Admin can view incidents and responder states.
- System avoids duplicate assignment where possible.

### Milestone 10: Real-Time Tracking and Communication

Goal: Support live response coordination.

Deliverables:

- Live location sharing
- Responder ETA
- In-app status updates
- Voice command prototype
- Emergency contact notification
- Incident media upload
- Basic chat or structured updates

Acceptance Criteria:

- User sees aid en route after acceptance.
- Responder sees incident location and required details.
- ETA updates as location changes.
- App supports hands-free critical commands for emergency request and navigation.

### Milestone 11: Offline and Low-Connectivity Support

Goal: Make FastAid useful in rural or weak-network areas.

Deliverables:

- Offline mode
- Cached emergency contacts
- Cached map area or last-known area
- SMS SOS fallback
- Queue-and-sync incident reports
- Low-bandwidth incident payload

Acceptance Criteria:

- User can trigger a basic SOS without stable internet.
- App can send location and incident type through SMS when configured.
- Queued reports sync when connectivity returns.
- Offline UI explains what has and has not been sent.

### Milestone 12: Safety, Privacy, and Compliance

Goal: Prepare the app for high-trust emergency use.

Deliverables:

- Privacy policy draft
- Consent and location policy
- Data retention policy
- Emergency disclaimer
- Abuse prevention plan
- Responder verification rules
- Security review checklist

Acceptance Criteria:

- Sensitive location and medical data are protected.
- Users understand when official emergency services have not been contacted.
- App cannot silently expose user location without consent.
- Incident logs are available for accountability.

### Milestone 13: Testing and Validation

Goal: Test usability, reliability, and service logic.

Deliverables:

- Usability testing plan
- Field simulation test
- API tests
- Notification tests
- Offline tests
- Accessibility checks
- Load and latency baseline

Acceptance Criteria:

- Emergency request can be completed quickly under simulated stress.
- Nearby search and routing stay within acceptable latency.
- Notification failure paths are tested.
- App remains usable with poor network.

### Milestone 14: Pilot Launch

Goal: Run a controlled launch with limited partners.

Deliverables:

- Pilot region configuration
- Partner responder list
- Training material
- Admin operations guide
- Incident monitoring dashboard
- Feedback loop

Acceptance Criteria:

- Pilot works in one city/region with selected responders and service partners.
- All test incidents are auditable.
- Support process exists for false alarms, failed notifications, and responder disputes.

### Milestone 15: Multi-Country Expansion

Goal: Make FastAid configurable across countries and regions.

Deliverables:

- Country configuration model
- Emergency number registry
- Local POI category mapping
- Language and units settings
- Local legal/compliance checklist
- Provider abstraction for maps and notifications

Acceptance Criteria:

- Country-specific emergency numbers are configurable.
- Maps provider and place category mapping are not hardcoded.
- App can support different languages, distance units, and emergency workflows.
- Official dispatch integrations can be added per country where available.

## MVP Scope

Include:

- Login/signup
- Live map
- One-tap SOS
- Accident report
- Breakdown assistance
- Fuel assistance
- Nearby aid categories
- Google Places POI discovery
- Route/ETA display
- Verified responder notification demo
- Responder accept/decline
- User live tracking view
- Profile and emergency contacts
- Offline/SMS fallback concept

Exclude from MVP:

- Full government emergency dispatch integration
- Insurance claims
- Payments
- Hospital bed availability unless partner data exists
- Medical diagnosis
- Fully automated emergency decision-making

## Suggested App Screens

1. Splash / brand
2. Login / signup
3. Permission request
4. Home map
5. One-tap SOS confirmation
6. Accident report
7. Breakdown report
8. Fuel request
9. Nearby aid list
10. POI detail sheet
11. Verified responder detail sheet
12. Live incident tracking
13. Responder alert
14. Responder route view
15. Incident resolved
16. Profile
17. Emergency contacts
18. Settings
19. Offline mode
20. Help/tutorial
21. Admin incident dashboard
22. Partner onboarding

## POI Category Mapping

Medical:

- `hospital`
- `general_hospital`
- `medical_center`
- `medical_clinic`
- `pharmacy`
- `doctor`

Official emergency:

- `police`
- `fire_station`

Roadside assistance:

- `car_repair`
- `tire_shop`
- `gas_station`
- `electric_vehicle_charging_station`
- `parking`
- `rest_stop`

Useful nearby support:

- `atm`
- `convenience_store`
- `hardware_store`
- `lodging`
- `public_bathroom`

## Key Design Principle

FastAid should use real maps data to understand the world around the user, but it should use verified networks and official integrations to promise emergency response.

## Research Sources To Reference

- Google Places API Nearby Search: https://developers.google.com/maps/documentation/places/web-service/nearby-search
- Google Places API Place Types: https://developers.google.com/maps/documentation/places/web-service/place-types
- Google Routes API: https://developers.google.com/maps/documentation/routes/compute_route_directions
- Google Maps Platform Terms: https://cloud.google.com/maps-platform/terms
- India 112 Emergency Response Support System: https://112.gov.in/
