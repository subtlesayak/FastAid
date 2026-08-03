# FastAid Project

GSD Core Version: 1.9.1

## Current Milestone: v1.2 Pilot Operations

**Goal:** Make the controlled FastAid pilot operable by real administrators and partners without overstating production or official-dispatch readiness.

**Target features:**

- Authenticated partner and incident operations console
- Remote responder and emergency-contact notification delivery
- Managed persistence, durable alert jobs, monitoring, and recovery
- Live-key device UAT followed by a staged, auditable pilot exercise

**Build order:** Begin with the operations console on the verified v1.1 API contract, then add delivery and durable infrastructure before field UAT.


## Completed Milestone: v1.1 Pilot Backend Foundations

**Goal:** Replace volatile prototype dispatch data with persistent, authenticated, auditable pilot services.

**Status:** Completed and audited 2026-07-13. All 19 requirements are satisfied; production-scale infrastructure remains deferred.

**Target features:**
- Persistent incident, alert, and responder lifecycle data
- Pending-to-verified responder onboarding and availability
- Protected responder/admin operations with audit history
- Deterministic matching and notification adapter
- HTTPS deployment and field-UAT readiness


## Active Android Reliability Work

Native Android hardening completed on 2026-07-13:

- Native Google Maps and Places SDK architecture; nearby public data no longer depends on a developer laptop
- Real-result-only cache with no fabricated POI fallback
- Current-location-first startup and explicit saved-location behavior
- Material 3 navigation, filters, recovery states, safety profile, and settings
- On-device Home, Nearby, Profile, insets, and crash-buffer verification

The remaining gate is live-key UAT: add the Android package/SHA-1-restricted key, verify map tiles and live Places on the physical device, then record screenshots and open/call/navigation results.

## Vision

FastAid is a map-centric emergency assistance platform for accidents, roadside emergencies, breakdowns, fuel issues, and nearby urgent support. It uses real maps and POI data to identify nearby hospitals, police stations, fire stations, repair centers, petrol pumps, pharmacies, EV charging points, and other relevant services across countries.

FastAid should work as a global service concept: maps and place data provide local awareness, while official emergency dispatch and verified responder notifications are handled through country-specific integrations, verified partners, or FastAid's own responder network.

## Problem

During accidents and roadside emergencies, victims and bystanders often struggle to identify the right help, communicate accurate location details, avoid duplicate calls, and know whether aid is actually coming. First responders and roadside assistance providers also suffer from incomplete location data, repeated dispatches, missing incident details, and delayed coordination.

## Product Principle

Use maps data to discover nearby aid. Use verified responder networks and official integrations to request actual help.

## Target Users

1. Victims and bystanders
2. Ambulance drivers and medical responders
3. Police/fire/official responders
4. Roadside repair and towing partners
5. Petrol pump and EV charging partners
6. Hospitals, clinics, and pharmacies
7. Dispatch or operations admins

## Service Modes

1. Accident emergency
2. Medical emergency
3. Vehicle breakdown
4. Fuel assistance
5. Fire or police emergency
6. Unknown SOS
7. Offline/low-network SOS

## Core Outcomes

1. A user can trigger emergency help quickly.
2. A user can find the nearest relevant aid by category.
3. FastAid can notify verified nearby responders or partners.
4. Responders can accept, decline, and navigate to the incident.
5. Users can see aid status, ETA, and live movement where available.
6. The app can degrade gracefully in low-connectivity areas.
7. The product can be configured for different countries.

## Non-Negotiables

1. Always provide a direct emergency-call fallback.
2. Never imply official dispatch unless official integration exists.
3. Clearly separate map-discovered places from verified FastAid responders.
4. Protect location, medical, contact, and incident data.
5. Keep the emergency UI simple enough to use under stress.

## MVP Definition

The MVP demonstrates:

1. Live map with user location
2. Nearby aid discovery by category
3. One-tap SOS and report flows
4. Accident, breakdown, and fuel assistance forms
5. Verified responder matching simulation
6. Responder accept/decline flow
7. ETA and live status view
8. Profile, emergency contacts, and basic settings
9. Offline/SMS SOS fallback concept

## Out Of Scope For MVP

1. Full government dispatch integration
2. Automated medical diagnosis
3. Payments, insurance, and claims
4. Guaranteed hospital bed availability
5. Fully autonomous emergency routing decisions


## Evolution

This document evolves at phase transitions and milestone boundaries. Requirements, decisions, constraints, and validated outcomes are updated after each verified phase.

---
*Last updated: 2026-07-16 for v1.2 Pilot Operations; live-key UAT remains pending*
