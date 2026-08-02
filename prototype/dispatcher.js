const $ = (selector) => document.querySelector(selector);

const state = {
  currentAlertId: null,
  currentIncidentId: null
};

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2400);
}

async function getJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.json();
}

function setApiOffline() {
  $("#serverStatus").textContent = "API Offline";
  $("#serverStatus").classList.add("offline");
  $("#mapsProvider").textContent = "Unavailable";
}

async function loadHealth() {
  try {
    const health = await getJson("/api/health");
    $("#serverStatus").textContent = "API Online";
    $("#serverStatus").classList.remove("offline");
    $("#mapsProvider").textContent = health.mapsProvider;
  } catch {
    setApiOffline();
  }
}

async function loadCountries() {
  try {
    const countries = await getJson("/api/countries");
    const entries = Object.entries(countries);
    $("#countrySummary").textContent = `${entries.length} configs`;
    $("#countryList").innerHTML = entries.map(([code, config]) => `
      <div class="country-row">
        <div>
          <strong>${config.country}</strong>
          <span>${code} - emergency ${config.emergencyNumber} - ${config.distanceUnit}</span>
        </div>
      </div>
    `).join("");
  } catch {
    $("#countrySummary").textContent = "Unavailable";
    $("#countryList").innerHTML = "";
  }
}

function sourceBadge(place) {
  const verified = place.source === "verified_responder" || place.verified;
  return `<span class="badge ${verified ? "verified" : "public"}">${verified ? "Verified" : "Public Place"}</span>`;
}

async function loadNearbyAid() {
  const incidentType = $("#incidentType").value;
  try {
    const data = await getJson(`/api/nearby/aid?incidentType=${encodeURIComponent(incidentType)}&lat=28.6328&lng=77.2197`);
    const results = data.results || [];
    $("#nearbyCount").textContent = String(results.length);
    $("#aidTable").innerHTML = results.map((place) => `
      <div class="aid-row">
        <div>
          <strong>${place.name}</strong>
          <span>${place.type} - ${place.distance || "nearby"} - ${place.eta || "ETA pending"}</span>
        </div>
        ${sourceBadge(place)}
      </div>
    `).join("");
  } catch {
    $("#nearbyCount").textContent = "0";
    $("#aidTable").innerHTML = "";
    showToast("Nearby aid API failed");
  }
}

async function createIncident(event) {
  event.preventDefault();
  const payload = {
    type: $("#incidentType").value,
    peopleCount: Number($("#peopleCount").value || 0),
    locationText: $("#locationText").value,
    note: $("#note").value
  };

  try {
    const data = await getJson("/api/incidents", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(payload)
    });
    state.currentIncidentId = data.incident.id;
    state.currentAlertId = data.alert.id;
    $("#alertId").textContent = data.alert.id;
    $("#currentIncident").textContent = `${data.incident.type} - ${data.incident.peopleCount} people`;
    $("#currentResponder").textContent = data.alert.responderName;
    $("#currentEta").textContent = `${data.alert.distance} - ${data.alert.eta}`;
    $("#currentStatus").textContent = data.alert.status;
    $("#alertStatus").textContent = data.alert.status;
    showToast("Incident created and responder alert generated");
  } catch {
    showToast("Incident API failed");
  }
}

async function decideAlert(decision) {
  if (!state.currentAlertId) {
    showToast("Create an incident first");
    return;
  }

  try {
    const data = await getJson(`/api/responder-alerts/${state.currentAlertId}/${decision}`, {
      method: "POST"
    });
    $("#currentStatus").textContent = data.incident?.status || data.alert.status;
    $("#alertStatus").textContent = data.alert.status;
    showToast(`Responder alert ${decision}ed`);
  } catch {
    showToast("Responder decision API failed");
  }
}

function bindEvents() {
  $("#incidentForm").addEventListener("submit", createIncident);
  $("#refreshAid").addEventListener("click", loadNearbyAid);
  $("#acceptAlert").addEventListener("click", () => decideAlert("accept"));
  $("#declineAlert").addEventListener("click", () => decideAlert("decline"));
}

bindEvents();
loadHealth();
loadCountries();
loadNearbyAid();
