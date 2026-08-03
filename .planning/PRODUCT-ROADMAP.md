# FastAid Roadmap

GSD Core Version: 1.9.1

## Milestone 0: Foundation

Status: Planned

Goal: Establish FastAid as a country-agnostic emergency assistance platform.

Phases:

1. Project charter and scope
2. Safety assumptions and risk model
3. Stakeholder and service-mode mapping
4. MVP boundary definition

Exit Criteria:

1. Product scope separates POI discovery from responder dispatch.
2. Emergency service modes include accident, medical, repair, fuel, police, fire, and offline SOS.
3. Country-specific behavior is configurable.

## Milestone 1: Requirements And Research

Status: Planned

Goal: Convert the capstone research into build-ready requirements.

Phases:

1. Personas and user journeys
2. Emergency scenario matrix
3. Functional requirements
4. Non-functional requirements
5. Safety and privacy requirements

Exit Criteria:

1. All user flows have acceptance criteria.
2. POI categories are mapped to user needs.
3. Requirements identify both user-facing and responder-facing workflows.

## Milestone 2: Maps And POI Feasibility

Status: Planned

Goal: Prove that real maps data can support global nearby-aid discovery.

Phases:

1. Maps provider setup
2. Nearby POI search spike
3. Place detail and contact lookup spike
4. Route and ETA spike
5. Provider abstraction design

Exit Criteria:

1. App can query nearby hospitals, police, fire stations, petrol pumps, repair centers, tire shops, pharmacies, and EV charging points.
2. Results are ranked by relevance, distance, travel time, and availability when possible.
3. The UI distinguishes verified responders from public POIs.

## Milestone 3: UX And UI System

Status: Planned

Goal: Design a stress-friendly emergency interface.

Phases:

1. Information architecture
2. Low-fidelity user flows
3. High-fidelity visual system
4. User app screens
5. Responder app screens
6. Admin/dispatch screens
7. Offline and failure states

Exit Criteria:

1. User can request help in one tap.
2. Manual reporting requires minimal input.
3. Emergency call, verified responder notification, and nearby POI discovery are visibly separate actions.
4. UI supports high-pressure usage, outdoor readability, and accessibility.

## Milestone 4: MVP App Shell

Status: Planned

Goal: Build the first working mobile app experience.

Phases:

1. App project setup
2. Authentication
3. Location permission
4. Map screen
5. Nearby aid filters
6. Incident report forms
7. Profile and settings
8. Offline fallback UI

Exit Criteria:

1. User can log in and see their location on the map.
2. User can browse nearby aid categories.
3. User can create a test incident.
4. App handles denied permissions and low-network state gracefully.

## Milestone 5: Backend And Dispatch Layer

Status: Planned

Goal: Build the service layer required for notification and coordination.

Phases:

1. API design
2. Database schema
3. Incident lifecycle
4. Verified responder accounts
5. Partner onboarding
6. Matching and notification service
7. Admin dispatch dashboard

Exit Criteria:

1. Incident can be created and stored.
2. Verified responders can receive alerts.
3. Responders can accept or decline.
4. User can see accepted responder and status.
5. Admin can monitor incidents and responders.

## Milestone 6: Real-Time Response

Status: Planned

Goal: Enable live coordination after an incident is accepted.

Phases:

1. Live responder tracking
2. ETA updates
3. Status updates
4. Emergency contact notifications
5. Media upload
6. Structured responder/user communication

Exit Criteria:

1. User sees aid en route.
2. Responder sees incident details and navigation.
3. ETA updates during the response.
4. Incident status can be resolved or cancelled.

## Milestone 7: Offline And Low-Connectivity

Status: Planned

Goal: Make FastAid useful in rural and weak-network areas.

Phases:

1. Offline mode
2. Cached emergency contacts
3. Cached map area or last-known area
4. SMS SOS fallback
5. Queue-and-sync reports
6. Low-bandwidth payloads

Exit Criteria:

1. User can trigger a basic SOS without stable data.
2. App clearly shows whether the alert was sent, queued, or failed.
3. Queued reports sync when connectivity returns.

## Milestone 8: Safety, Compliance, And Pilot

Status: Planned

Goal: Prepare FastAid for real-world testing with partners.

Phases:

1. Privacy and data policy
2. Responder verification rules
3. Abuse prevention
4. Audit logs
5. Pilot region configuration
6. Training and operations guide
7. Field simulation testing

Exit Criteria:

1. Pilot can run with verified partners in one region.
2. False alarms, failed notifications, and duplicate dispatches have handling rules.
3. All incidents are auditable.

## Milestone 9: Multi-Country Expansion

Status: Planned

Goal: Make FastAid configurable across countries and map providers.

Phases:

1. Country configuration model
2. Emergency number registry
3. Local language and unit settings
4. Local POI category mapping
5. Official emergency integration adapters
6. Maps provider abstraction

Exit Criteria:

1. Country-specific emergency numbers are configurable.
2. App supports multiple languages and distance units.
3. Maps and notification providers can vary by region.
4. Official dispatch integrations can be added without redesigning the product.
