---
phase: 10
verified: 2026-07-13
status: passed
asvs_level: 1
register_authored_at_plan_time: false
threats_found: 5
threats_closed: 5
threats_open: 0
---

# Phase 10 Security Verification

## Retroactive STRIDE Register

| ID | Category | Threat | Severity | Mitigation Evidence | Status |
|----|----------|--------|----------|---------------------|--------|
| P10-T1 | Information Disclosure | Server Maps/admin/provider secret packaged in APK | High | Artifact scanner passed against built APK; Android key separate | Closed |
| P10-T2 | Information Disclosure | Remote API traffic sent over cleartext | High | Android base cleartext disabled; only loopback/emulator domains allowed | Closed |
| P10-T3 | Spoofing | Misconfigured service claims pilot readiness | High | Pilot readiness requires HTTPS, Maps key, active admin, storage, and delivery mode | Closed |
| P10-T4 | Tampering | Local secret/data files committed | High | Ignore rules plus tracked-file artifact checks | Closed |
| P10-T5 | Denial of Service | Anonymous incident spam | Medium | Per-client fixed-window incident limiter, bounded body, bounded input fields | Closed |

## Accepted Risks

- TLS terminates at the deployment proxy; private proxy-to-process transport is trusted.
- Local notification mode is simulated and prominently reported in readiness/UAT.
- Single-process JSON persistence is not suitable for public or multi-replica service.

## Audit Trail

Retroactive ASVS L1 review completed inline. No high-severity threats remain open; the artifact gate and Android build both passed.
