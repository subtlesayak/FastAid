const assert = require("node:assert/strict");
const test = require("node:test");
const { findDuplicateIncidentSuggestions } = require("./duplicates");

function incident(overrides = {}) {
  return {
    id: overrides.id || "incident",
    type: overrides.type || "Accident",
    status: overrides.status || "notified",
    locationText: overrides.locationText || "MG Road",
    location: overrides.location === undefined ? { lat: 12.9716, lng: 77.5946 } : overrides.location,
    createdAt: overrides.createdAt || "2026-07-16T10:00:00.000Z"
  };
}

test("suggests nearby same-type incidents without mutating them", () => {
  const incidents = [
    incident({ id: "new", createdAt: "2026-07-16T10:05:00.000Z" }),
    incident({ id: "old", location: { lat: 12.972, lng: 77.5948 } })
  ];

  const suggestions = findDuplicateIncidentSuggestions(incidents);

  assert.equal(suggestions.length, 1);
  assert.deepEqual(suggestions[0].incidentIds, ["new", "old"]);
  assert.equal(suggestions[0].confidence, "high");
  assert.ok(suggestions[0].distanceMeters < 250);
  assert.equal(incidents[0].status, "notified");
});

test("labels nearby reports with different types as possible", () => {
  const suggestions = findDuplicateIncidentSuggestions([
    incident({ id: "medical", type: "Medical", createdAt: "2026-07-16T10:02:00.000Z" }),
    incident({ id: "accident", type: "Accident" })
  ]);

  assert.equal(suggestions.length, 1);
  assert.equal(suggestions[0].confidence, "possible");
});

test("does not match generic location labels without coordinates", () => {
  const suggestions = findDuplicateIncidentSuggestions([
    incident({ id: "a", locationText: "Current location", location: null }),
    incident({ id: "b", locationText: "Current location", location: null })
  ]);

  assert.deepEqual(suggestions, []);
});

test("excludes closed, old, and geographically distant reports", () => {
  const suggestions = findDuplicateIncidentSuggestions([
    incident({ id: "active" }),
    incident({ id: "resolved", status: "resolved" }),
    incident({ id: "old", createdAt: "2026-07-16T09:00:00.000Z" }),
    incident({ id: "far", location: { lat: 13.1, lng: 77.8 } })
  ]);

  assert.deepEqual(suggestions, []);
});
