# FastAid Pilot Field UAT

## Preconditions

- Backend deployed at the configured HTTPS URL with `GET /api/readiness` returning `200`.
- Android build uses a package/SHA-1-restricted Maps key and the HTTPS `FASTAID_BACKEND_URL`.
- One administrator account exists.
- Two test responder accounts are verified, located within the test area, and set to `available`.
- Test participants understand that FastAid is not connected to official emergency dispatch.
- Use a staged scenario only. Do not call `112` during the test.

## Journey

1. Launch FastAid and grant precise location permission.
2. Confirm the live Google map centers on the device and the location control recenters it.
3. Open **Nearby Aid** and verify results use current Google Places data, show distance/open status when supplied, and expose **Call** only when a phone number exists.
4. Use **Go** on one result and confirm Google Maps opens with the selected coordinates.
5. Return to FastAid and hold SOS for the cancellable countdown; cancel once and confirm no incident is created.
6. Repeat the SOS flow and allow the staged incident to be created.
7. Confirm the nearest compatible verified responder receives attempt 1 and becomes `busy`; confirm public POIs are not listed as dispatch recipients.
8. Decline attempt 1 from that responder's authenticated test session.
9. Confirm the responder returns to `available`, attempt 1 becomes `declined`, and attempt 2 reaches the next eligible responder with only one active alert.
10. Accept attempt 2 and confirm the incident becomes `en_route`.
11. Advance the incident to `arrived`, then `resolved`; confirm the active alert closes and the responder returns to `available`.
12. As administrator, inspect `/api/admin/audit` and `/api/admin/notifications` for actor, target, lifecycle transitions, provider, and delivery attempts.
13. Disable network access and confirm offline SMS/queue guidance is shown without claiming successful responder dispatch.
14. Restore connectivity and confirm the app refreshes live Places data.

## Pass Criteria

- No public Google place is presented as a verified dispatchable responder.
- There is never more than one active alert for the staged incident.
- Decline, acceptance, and resolution state persist after a backend restart.
- All protected mutations have audit events and every delivery attempt is inspectable.
- No server key, admin password, bearer token, or pilot data appears in source control or the APK artifact gate.

Record device model, Android version, app version, backend version, timestamps, screenshots, failed steps, and tester initials with the UAT result.
