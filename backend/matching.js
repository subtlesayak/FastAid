function toRadians(degrees) {
  return degrees * Math.PI / 180;
}

function hasCoordinate(location) {
  return location
    && Number.isFinite(Number(location.lat))
    && Number.isFinite(Number(location.lng));
}

function distanceKmBetween(a, b) {
  if (!hasCoordinate(a) || !hasCoordinate(b)) return Number.POSITIVE_INFINITY;
  const aLat = Number(a.lat);
  const aLng = Number(a.lng);
  const bLat = Number(b.lat);
  const bLng = Number(b.lng);
  const earthRadiusKm = 6371;
  const dLat = toRadians(bLat - aLat);
  const dLng = toRadians(bLng - aLng);
  const lat1 = toRadians(aLat);
  const lat2 = toRadians(bLat);
  const haversine = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return 2 * earthRadiusKm * Math.asin(Math.sqrt(haversine));
}

function rankResponders({ incident, responders, compatibleTypes, excludeResponderIds = [] }) {
  if (!hasCoordinate(incident?.location)) return [];
  const excluded = new Set(excludeResponderIds);
  const serviceRanks = new Map((compatibleTypes || []).map((type, index) => [type, index]));

  return (responders || [])
    .filter((responder) => responder.verificationStatus === "verified")
    .filter((responder) => responder.availabilityStatus === "available")
    .filter((responder) => responder.source !== "public_place")
    .filter((responder) => serviceRanks.has(responder.responderType))
    .filter((responder) => !excluded.has(responder.id))
    .map((responder) => {
      const distanceKm = distanceKmBetween(incident.location, responder.location);
      const configuredRadius = Number(responder.serviceRadiusKm);
      const serviceRadiusKm = Number.isFinite(configuredRadius) && configuredRadius > 0
        ? configuredRadius
        : 10;
      return {
        responder,
        distanceKm,
        serviceRadiusKm,
        serviceRank: serviceRanks.get(responder.responderType)
      };
    })
    .filter((candidate) => Number.isFinite(candidate.distanceKm))
    .filter((candidate) => candidate.distanceKm <= candidate.serviceRadiusKm)
    .sort((a, b) => (
      a.serviceRank - b.serviceRank
      || a.distanceKm - b.distanceKm
      || a.responder.id.localeCompare(b.responder.id)
    ));
}

function formatDistance(km) {
  if (!Number.isFinite(km)) return "Nearby";
  if (km < 1) return `${Math.max(50, Math.round(km * 1000 / 50) * 50)} m`;
  return `${km < 10 ? km.toFixed(1) : Math.round(km)} km`;
}

function estimateEta(km) {
  if (!Number.isFinite(km)) return "ETA pending";
  return `${Math.max(2, Math.round(km / 0.45))} min`;
}

module.exports = {
  distanceKmBetween,
  estimateEta,
  formatDistance,
  rankResponders
};
