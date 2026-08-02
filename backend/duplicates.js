const { distanceKmBetween } = require("./matching");

const closedStatuses = new Set(["resolved", "cancelled", "failed"]);
const genericLocations = new Set(["", "current location", "location unavailable", "unknown"]);

function normalizeText(value) {
  return String(value || "").trim().toLowerCase().replace(/\s+/g, " ");
}

function createdTime(incident) {
  const value = new Date(incident?.createdAt || "").getTime();
  return Number.isFinite(value) ? value : null;
}

function findDuplicateIncidentSuggestions(incidents, options = {}) {
  const windowMinutes = boundedNumber(options.windowMinutes, 15, 1, 120);
  const radiusMeters = boundedNumber(options.radiusMeters, 250, 25, 2_000);
  const limit = boundedNumber(options.limit, 100, 1, 500);
  const timeWindowMs = windowMinutes * 60_000;
  const radiusKm = radiusMeters / 1_000;

  const candidates = (incidents || [])
    .filter((incident) => incident && incident.id && !closedStatuses.has(incident.status))
    .map((incident) => ({ incident, time: createdTime(incident) }))
    .filter((entry) => entry.time !== null)
    .sort((a, b) => b.time - a.time)
    .slice(0, 250);

  const suggestions = [];
  for (let newerIndex = 0; newerIndex < candidates.length; newerIndex += 1) {
    for (let olderIndex = newerIndex + 1; olderIndex < candidates.length; olderIndex += 1) {
      const newer = candidates[newerIndex];
      const older = candidates[olderIndex];
      const timeDeltaMs = newer.time - older.time;
      if (timeDeltaMs > timeWindowMs) break;

      const newerText = normalizeText(newer.incident.locationText);
      const olderText = normalizeText(older.incident.locationText);
      const exactNamedLocation = newerText === olderText && !genericLocations.has(newerText);
      const distanceKm = distanceKmBetween(newer.incident.location, older.incident.location);
      const hasComparableCoordinates = Number.isFinite(distanceKm);
      const nearbyCoordinates = hasComparableCoordinates && distanceKm <= radiusKm;
      if (hasComparableCoordinates ? !nearbyCoordinates : !exactNamedLocation) continue;

      const sameType = normalizeText(newer.incident.type) === normalizeText(older.incident.type);
      const distanceMeters = Number.isFinite(distanceKm) ? Math.round(distanceKm * 1_000) : null;
      suggestions.push({
        id: `duplicate_${newer.incident.id}_${older.incident.id}`,
        incidentIds: [newer.incident.id, older.incident.id],
        confidence: sameType ? "high" : "possible",
        reason: duplicateReason({ sameType, exactNamedLocation, distanceMeters }),
        timeDeltaMinutes: Number((timeDeltaMs / 60_000).toFixed(1)),
        distanceMeters,
        createdAt: newer.incident.createdAt
      });
    }
  }

  return suggestions
    .sort((a, b) => (
      confidenceRank(a.confidence) - confidenceRank(b.confidence)
      || a.timeDeltaMinutes - b.timeDeltaMinutes
      || String(b.createdAt).localeCompare(String(a.createdAt))
    ))
    .slice(0, limit);
}

function duplicateReason({ sameType, exactNamedLocation, distanceMeters }) {
  const typeText = sameType ? "same incident type" : "different reported types";
  if (distanceMeters !== null) {
    return `${typeText}, reported ${distanceMeters} m apart`;
  }
  if (exactNamedLocation) return `${typeText}, same named location`;
  return typeText;
}

function confidenceRank(confidence) {
  return confidence === "high" ? 0 : 1;
}

function boundedNumber(value, fallback, minimum, maximum) {
  if (value === null || value === undefined || value === "") return fallback;
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.max(minimum, Math.min(maximum, parsed));
}

module.exports = { findDuplicateIncidentSuggestions };
