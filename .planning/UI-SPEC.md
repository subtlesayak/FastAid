# FastAid UI Specification

GSD Core Version: 1.9.1

## Design Goal

FastAid must feel immediate, calm, and operational. The UI is not a marketing surface; it is an emergency tool. The first screen after login should be a focused SOS action hub with fast access to nearby aid; live map context belongs to the dedicated Map tab.

## Interface Principles

1. Prioritize one-tap emergency action.
2. Keep location state visible during emergency actions and use the dedicated Map tab whenever spatial context matters.
3. Use large targets and short labels.
4. Prefer icons plus concise text for emergency categories.
5. Avoid long forms during urgent flows.
6. Show system state clearly: locating, sending, notified, accepted, en route, arrived, failed, offline.
7. Never hide the direct emergency-call option.

## Navigation Model

Bottom tabs:

1. SOS
2. Map
3. Nearby
4. Incidents
5. Profile

Emergency overlay actions:

1. Call emergency number
2. Notify FastAid responders
3. Find nearby aid
4. Share location

## Visual Language

Primary colors:

1. Emergency red: urgent action and alerts
2. Responder blue: verified responder, accepted aid, routing
3. Support green: available repair/fuel/help
4. Warning amber: pending, waiting, weak network
5. Neutral gray: unavailable, disabled, cancelled

Typography:

1. Large labels for emergency actions
2. Compact labels for map cards
3. High-contrast body text
4. No decorative type in critical flows

Icon categories:

1. Medical cross for hospitals/clinics
2. Shield or siren for police
3. Flame for fire
4. Fuel pump for petrol stations
5. Wrench for repair
6. Tire for tire shops
7. Battery/plug for EV charging
8. Pill for pharmacies
9. Tow hook/truck for towing

## Core User Screens

### 1. Login / Signup

Purpose: Secure entry without visual clutter.

Content:

1. FastAid logo
2. Log in
3. Sign up
4. Emergency call shortcut

States:

1. Loading
2. Invalid credentials
3. Offline limited mode

### 2. Permission Setup

Purpose: Explain and request location, notifications, and optional emergency contacts.

Content:

1. Location permission
2. Notification permission
3. Emergency contact import
4. Skip with limited mode

### 3. SOS Home

Purpose: Primary emergency action surface without map clutter.

Content:

1. Current location summary
2. Circular SOS action
3. Circular current-location action
4. Circular emergency-call action
5. Circular share-location action
6. Nearby aid chips
7. Incident status
8. Bottom navigation

Important behavior:

1. If location is unavailable, show manual location input.
2. If network is weak, show offline/SMS fallback.
3. Live map context remains available from the dedicated Map tab.

### 4. SOS Confirmation

Purpose: Prevent accidental alerts while staying fast.

Content:

1. Countdown timer
2. Cancel button
3. Send now button
4. Call emergency number button
5. Incident type quick chips

### 5. Accident Report

Purpose: Collect essential incident data.

Fields:

1. Location
2. Incident type
3. Number of people
4. Severity estimate
5. Media upload
6. Notes or voice input

Required fields:

1. Location
2. Incident type

### 6. Breakdown / Fuel Request

Purpose: Route non-medical roadside emergencies to relevant aid.

Fields:

1. Location
2. Issue type
3. Vehicle type
4. Fuel type if relevant
5. Need towing
6. Notes or voice input

### 7. Nearby Aid

Purpose: Show useful places and verified help near the user.

Categories:

1. Hospitals
2. Clinics
3. Pharmacies
4. Police
5. Fire stations
6. Petrol pumps
7. Repair centers
8. Tire shops
9. EV charging
10. Towing partners
11. Public toilets
12. Rest stops
13. Parking
14. Medical laboratories
15. Auto-parts stores

Each result card:

1. Name
2. Category
3. Distance
4. ETA
5. Open/closed if available
6. Verified badge if FastAid partner
7. Call
8. Directions
9. Request help if verified

### 8. Live Incident Tracking

Purpose: Reassure the user and reduce repeated calls.

Content:

1. Incident status
2. Responder name or unit
3. ETA
4. Live route
5. Call responder or dispatch if enabled
6. Share status with emergency contact
7. Cancel or mark resolved

### 9. Profile

Purpose: Store data useful in emergencies.

Content:

1. Name
2. Phone
3. Emergency contacts
4. Medical conditions
5. Blood group
6. Vehicle details
7. Preferred language

### 10. Settings

Purpose: Control operational preferences.

Content:

1. Offline mode
2. Voice commands
3. SMS SOS
4. Background location
5. Accessibility
6. Map downloads
7. Data privacy

## Responder Screens

### 1. Responder Home

Content:

1. Availability toggle
2. Current location
3. Assigned incidents
4. Nearby open incidents

### 2. Incoming Alert

Content:

1. Incident type
2. Distance
3. ETA
4. Number of people
5. Location
6. Accept
7. Decline
8. Request more info

### 3. Route To Incident

Content:

1. Map route
2. Turn-by-turn navigation handoff
3. Incident details
4. Update status
5. Arrived button

### 4. Resolve Incident

Content:

1. Outcome
2. Notes
3. Handoff information
4. Mark resolved

## Admin Screens

### 1. Incident Dashboard

Content:

1. Active incidents
2. Severity
3. Location
4. Assigned responder
5. Status
6. Time since report
7. Duplicate incident warning

### 2. Partner Management

Content:

1. Verified responders
2. Hospitals
3. Repair centers
4. Petrol pumps
5. Towing partners
6. Verification status

## Critical States

1. No location permission
2. GPS unavailable
3. No network
4. Alert sent
5. Alert failed
6. No verified responder available
7. Nearby POIs found but not verified
8. Emergency number unavailable for country config
9. Duplicate incident suspected

## Accessibility Requirements

1. Large touch targets
2. High contrast emergency buttons
3. Screen-reader labels
4. Voice command support for critical actions
5. No reliance on color alone
6. Reduced-motion mode
7. Local language support
