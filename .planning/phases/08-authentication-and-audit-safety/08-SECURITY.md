---
phase: 8
verified: 2026-07-13
status: passed
asvs_level: 1
register_authored_at_plan_time: false
threats_found: 5
threats_closed: 5
threats_open: 0
---

# Phase 8 Security Verification

## Retroactive STRIDE Register

| ID | Category | Threat | Severity | Mitigation Evidence | Status |
|----|----------|--------|----------|---------------------|--------|
| P8-T1 | Information Disclosure | Passwords or bearer tokens stored raw | High | Salted `scrypt`; SHA-256 session token hashes; storage assertions | Closed |
| P8-T2 | Elevation of Privilege | Public signup creates admin | High | Public role allowlist; environment-only bootstrap; integration test | Closed |
| P8-T3 | Elevation of Privilege | Responder changes another profile/alert/incident | High | Role and ownership checks with 403 integration coverage | Closed |
| P8-T4 | Repudiation | Protected state changes lack actor evidence | Medium | Append-only audit events with actor/role/action/target/timestamp | Closed |
| P8-T5 | Denial of Service | Credential brute-force/signup abuse | Medium | Per-client fixed-window auth limiter and bounded request body | Closed |

## Accepted Risks

- Rate limits are process-local and reset on restart; a distributed gateway limit is required before public launch.
- Audit events are append-only through the application but not cryptographically chained against host-level tampering.

## Audit Trail

Retroactive ASVS L1 review completed inline. No high-severity threats remain open.
