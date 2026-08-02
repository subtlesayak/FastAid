const baseUrl = process.env.FASTAID_BASE_URL || "http://localhost:4173";

async function request(path, options) {
  const response = await fetch(`${baseUrl}${path}`, options);
  const text = await response.text();
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    body = text;
  }

  if (!response.ok) {
    throw new Error(`${path} failed with ${response.status}: ${text}`);
  }

  return body;
}

async function main() {
  const health = await request("/api/health");
  const countries = await request("/api/countries");
  const nearby = await request("/api/nearby/aid?incidentType=Breakdown");
  const created = await request("/api/incidents", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      type: "Breakdown",
      peopleCount: 1,
      locationText: "Connaught Place, New Delhi",
      note: "Smoke test incident"
    })
  });
  const accepted = await request(`/api/responder-alerts/${created.alert.id}/accept`, {
    method: "POST"
  });

  const summary = {
    health: health.ok === true,
    mapsProvider: health.mapsProvider,
    countries: Object.keys(countries).length,
    nearbyResults: nearby.results.length,
    incidentStatus: accepted.incident.status,
    alertStatus: accepted.alert.status
  };

  console.log(JSON.stringify(summary, null, 2));

  if (summary.incidentStatus !== "en_route" || summary.alertStatus !== "accepted") {
    throw new Error("Responder accept flow did not reach expected state");
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
