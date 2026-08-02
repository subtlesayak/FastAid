# FastAid Safety Model

FastAid is designed around a conservative safety boundary: official emergency services come first, public aid discovery comes second, and responder dispatch is restricted to verified pilot participants.

## User-Facing Priority

1. Call the official emergency number.
2. Share or confirm location.
3. Discover nearby public aid.
4. Create a controlled pilot incident only when a verified responder workflow is configured.

## Data Source Labels

| Source | Meaning | Dispatchable |
|---|---|---|
| `official_number` | Country or region emergency number | Yes, through phone dialer only |
| `public_place` | Google Places result | No |
| `cached_place` | Previously fetched Google Places result | No |
| `verified_responder` | Authenticated pilot responder | Only inside controlled pilot |
| `simulated_notification` | Local backend notification placeholder | No production claim |

## Failure States

FastAid should prefer clear fallback states over optimistic claims:

- `Live Places unavailable`
- `Saved nearby data`
- `No phone number supplied`
- `Location unavailable`
- `Responder service not configured`
- `Incident saved locally`

## Production Gaps

Before production use, FastAid needs:

- External security review
- Real notification adapters
- Multi-instance storage
- Incident audit retention policy
- Operations monitoring
- Regional legal review
- Verified responder onboarding and deactivation process
- Formal emergency-service partnership model
