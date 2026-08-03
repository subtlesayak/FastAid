# Phase 8 Context: Authentication And Audit Safety

## Boundary

Phase 8 protects responder and administrator operations without placing authentication in the critical public SOS path. Nearby Google Places discovery and incident creation remain usable without a session.

## Decisions

- Use Node's built-in `scrypt` with a unique random salt for password-equivalent credentials.
- Issue opaque, one-hour bearer sessions and persist only a SHA-256 token hash.
- Permit public signup only for `user` and `responder`; administrators are bootstrapped from deployment secrets.
- Link each responder account to one onboarding profile and restrict responder actions to that profile and its assigned alerts.
- Require administrator authorization for responder listing, verification changes, and audit access.
- Record protected state changes as append-only audit events containing actor, role, action, target, timestamp, and minimal before/after metadata.
- Keep the implementation dependency-free so the local Android testing workflow remains simple and deterministic.

## Safety Rules

- Never return credential hashes or session hashes in API responses.
- Never persist raw passwords or bearer tokens.
- Do not allow a public signup request to create an administrator.
- Do not allow pending, rejected, or suspended responders to publish availability.
- Do not expose all responder profiles or audit records to ordinary users.

## Deferred

- Password reset and email verification.
- OAuth or federated identity.
- Production database-backed session revocation.
- Operations dashboard UI.
