const assert = require("node:assert/strict");
const test = require("node:test");

const { distanceKmBetween, rankResponders } = require("./matching");

test("ranks eligible responders by service priority then distance", () => {
  const incident = { id: "inc_1", location: { lat: 28.6328, lng: 77.2197 } };
  const responders = [
    responder("police-near", "police", 28.6329),
    responder("hospital-far", "hospital", 28.64),
    responder("hospital-near", "hospital", 28.633)
  ];

  const ranked = rankResponders({
    incident,
    responders,
    compatibleTypes: ["hospital", "police"]
  });

  assert.deepEqual(ranked.map((candidate) => candidate.responder.id), [
    "hospital-near",
    "hospital-far",
    "police-near"
  ]);
  assert.ok(ranked[0].distanceKm < ranked[1].distanceKm);
});

test("excludes unverified, unavailable, public, attempted, incompatible, and out-of-radius records", () => {
  const incident = { id: "inc_1", location: { lat: 28.6328, lng: 77.2197 } };
  const eligible = responder("eligible", "car_repair", 28.633);
  const responders = [
    eligible,
    { ...responder("unverified", "car_repair", 28.633), verificationStatus: "pending" },
    { ...responder("busy", "car_repair", 28.633), availabilityStatus: "busy" },
    { ...responder("public", "car_repair", 28.633), source: "public_place" },
    { ...responder("out-of-radius", "car_repair", 29.0), serviceRadiusKm: 1 },
    responder("incompatible", "hospital", 28.633),
    responder("attempted", "car_repair", 28.633)
  ];

  const ranked = rankResponders({
    incident,
    responders,
    compatibleTypes: ["car_repair", "towing"],
    excludeResponderIds: ["attempted"]
  });

  assert.deepEqual(ranked.map((candidate) => candidate.responder.id), [eligible.id]);
});

test("returns no dispatch candidates without valid coordinates", () => {
  assert.deepEqual(rankResponders({
    incident: { id: "inc_1", location: null },
    responders: [responder("eligible", "hospital", 28.633)],
    compatibleTypes: ["hospital"]
  }), []);
  assert.equal(distanceKmBetween(null, { lat: 1, lng: 1 }), Number.POSITIVE_INFINITY);
});

function responder(id, responderType, lat) {
  return {
    id,
    name: id,
    responderType,
    verificationStatus: "verified",
    availabilityStatus: "available",
    source: "self_onboarding",
    location: { lat, lng: 77.2197 },
    serviceRadiusKm: 100
  };
}
