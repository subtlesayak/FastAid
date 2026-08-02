const categories = [
  { id: "accident", label: "Accident", icon: "+", color: "#f03535", types: ["hospital", "police", "fire_station", "pharmacy"] },
  { id: "breakdown", label: "Breakdown", icon: "R", color: "#fb8c00", types: ["car_repair", "tire_shop", "towing", "gas_station"] },
  { id: "fuel", label: "Fuel", icon: "F", color: "#25a645", types: ["gas_station", "electric_vehicle_charging_station", "towing"] },
  { id: "medical", label: "Medical", icon: "+", color: "#147fd7", types: ["hospital", "pharmacy"] },
  { id: "police", label: "Police", icon: "P", color: "#147fd7", types: ["police"] },
  { id: "fire", label: "Fire", icon: "F", color: "#f03535", types: ["fire_station"] },
  { id: "repair", label: "Repair", icon: "R", color: "#25a645", types: ["car_repair", "tire_shop", "towing"] },
  { id: "ev", label: "EV", icon: "E", color: "#25a645", types: ["electric_vehicle_charging_station"] }
];

const categoryToIncident = {
  accident: "Accident",
  breakdown: "Breakdown",
  fuel: "Fuel",
  medical: "Medical",
  police: "Police",
  fire: "Fire",
  repair: "Breakdown",
  ev: "Fuel"
};

const mockPlaces = [
  { id: 1, name: "City Hospital", type: "hospital", icon: "+", distance: "1.2 km", eta: "5 min", open: "Open 24 hrs", verified: true, source: "verified_responder", color: "#147fd7" },
  { id: 2, name: "Connaught Place Police Station", type: "police", icon: "P", distance: "0.8 km", eta: "4 min", open: "Open", verified: true, source: "verified_responder", color: "#147fd7" },
  { id: 3, name: "Speedy Auto Care", type: "car_repair", icon: "R", distance: "0.6 km", eta: "3 min", open: "Open", verified: false, source: "public_place", color: "#25a645" },
  { id: 4, name: "HP Petrol Pump", type: "gas_station", icon: "F", distance: "1.0 km", eta: "4 min", open: "Open 24 hrs", verified: false, source: "public_place", color: "#25a645" },
  { id: 5, name: "Tyre World", type: "tire_shop", icon: "T", distance: "1.4 km", eta: "7 min", open: "Open", verified: false, source: "public_place", color: "#25a645" },
  { id: 6, name: "FastAid Tow Partner", type: "towing", icon: "T", distance: "2.4 km", eta: "9 min", open: "Available", verified: true, source: "verified_responder", color: "#147fd7" }
];

let places = [...mockPlaces];

const demoLocation = {
  lat: 28.6328,
  lng: 77.2197,
  label: "Connaught Place, New Delhi",
  title: "Connaught Place",
  meta: "New Delhi, India",
  source: "demo"
};

const state = {
  selectedCategory: "accident",
  nearbyFilter: "all",
  sosTimer: null,
  sosRemaining: 0,
  offline: false,
  lastAlertId: null,
  apiProvider: "local mock",
  currentLocation: { ...demoLocation },
  locating: false
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));
const isServerMode = () => window.location.protocol !== "file:";

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2400);
}

function updateLocationUi() {
  const location = state.currentLocation || demoLocation;
  $("#currentLocation").textContent = location.title;
  const meta = $("#currentLocationMeta");
  if (meta) meta.textContent = location.meta;
  $("#locationInput").value = location.label;
  updateIncidentFromForm();
}

function setLocationFromCoordinates(coords) {
  const lat = Number(coords.latitude);
  const lng = Number(coords.longitude);
  const accuracy = Number.isFinite(Number(coords.accuracy)) ? Math.round(Number(coords.accuracy)) : null;
  const accuracyText = accuracy ? ` | accuracy ${accuracy} m` : "";

  state.currentLocation = {
    lat,
    lng,
    accuracy,
    label: `GPS ${lat.toFixed(5)}, ${lng.toFixed(5)}`,
    title: "Current GPS location",
    meta: `${lat.toFixed(5)}, ${lng.toFixed(5)}${accuracyText}`,
    source: "browser_geolocation"
  };
  updateLocationUi();
}

function requestBrowserLocation() {
  return new Promise((resolve, reject) => {
    const isLocalhost = ["localhost", "127.0.0.1", "::1"].includes(window.location.hostname);
    if (!navigator.geolocation) {
      reject(new Error("Geolocation is not supported in this browser."));
      return;
    }
    if (!window.isSecureContext && !isLocalhost) {
      reject(new Error("Geolocation needs HTTPS or localhost."));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 60000
    });
  });
}

function getLocationErrorMessage(error) {
  if (error?.code === 1) return "Location permission denied. Enable browser location for FastAid.";
  if (error?.code === 2) return "Current location unavailable. Using demo location.";
  if (error?.code === 3) return "Location request timed out. Try again near a clear signal.";
  return error?.message || "Could not read current location.";
}

function getNearbyQueryParams(incidentType) {
  const location = state.currentLocation || demoLocation;
  return new URLSearchParams({
    incidentType,
    countryCode: "IN",
    lat: String(location.lat),
    lng: String(location.lng)
  });
}

async function useCurrentLocation() {
  if (state.locating) return;
  const button = $("#locateButton");
  const previousText = button.textContent;
  state.locating = true;
  button.disabled = true;
  button.textContent = "...";
  showToast("Requesting browser location permission");

  try {
    const position = await requestBrowserLocation();
    setLocationFromCoordinates(position.coords);
    setPanel("nearby");
    await refreshNearbyAid({ incidentType: $("#incidentType").value });
    showToast("Nearby aid refreshed from your current GPS location");
  } catch (error) {
    showToast(getLocationErrorMessage(error));
  } finally {
    state.locating = false;
    button.disabled = false;
    button.textContent = previousText;
  }
}

function renderCategories() {
  $("#categoryGrid").innerHTML = categories.map((category) => `
    <button class="category-button ${category.id === state.selectedCategory ? "active" : ""}" type="button" data-category="${category.id}">
      <span class="category-icon" style="--cat:${category.color}">${category.icon}</span>
      <span>${category.label}</span>
    </button>
  `).join("");
}

function getFilteredPlaces() {
  const category = categories.find((item) => item.id === state.selectedCategory);
  const categoryPlaces = places.filter((place) => !category || category.types.includes(place.type));
  if (state.nearbyFilter === "verified") return categoryPlaces.filter((place) => place.verified);
  if (state.nearbyFilter === "public") return categoryPlaces.filter((place) => !place.verified);
  return categoryPlaces;
}

function placeColor(type, verified) {
  if (verified) return "#147fd7";
  if (["hospital", "fire_station", "pharmacy"].includes(type)) return "#f03535";
  if (["gas_station", "car_repair", "tire_shop", "electric_vehicle_charging_station", "towing"].includes(type)) return "#25a645";
  return "#147fd7";
}

function normalizeApiPlace(place) {
  return {
    id: place.id,
    name: place.name || "Nearby aid",
    type: place.type || "place",
    icon: place.icon || (place.type === "gas_station" ? "F" : place.type === "car_repair" ? "R" : "+"),
    distance: place.distance || "Nearby",
    eta: place.eta || "ETA pending",
    open: place.open || "Open status unknown",
    verified: Boolean(place.verified),
    source: place.source || (place.verified ? "verified_responder" : "public_place"),
    phone: place.phone || "",
    address: place.address || "",
    location: place.location || null,
    color: place.color || placeColor(place.type, place.verified)
  };
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[char]));
}

function getPlaceById(placeId) {
  return places.find((place) => String(place.id) === String(placeId));
}

function getPhoneHref(phone) {
  const cleanPhone = String(phone || "").replace(/[^+\d]/g, "");
  return cleanPhone ? `tel:${cleanPhone}` : "tel:112";
}

function getDirectionsUrl(place) {
  const origin = state.currentLocation || demoLocation;
  const hasCoordinates = Number.isFinite(Number(place.location?.lat))
    && Number.isFinite(Number(place.location?.lng));
  const destination = hasCoordinates
    ? `${place.location.lat},${place.location.lng}`
    : (place.address || place.name);
  const params = new URLSearchParams({
    api: "1",
    origin: `${origin.lat},${origin.lng}`,
    destination,
    travelmode: "driving"
  });
  return `https://www.google.com/maps/dir/?${params.toString()}`;
}

function callAid(place) {
  const hasPhone = Boolean(place.phone);
  window.location.href = getPhoneHref(place.phone);
  showToast(hasPhone ? `Calling ${place.name}` : `No listed phone for ${place.name}. Calling 112 fallback.`);
}

function routeToAid(place) {
  if (!place.location && !place.address && !place.name) {
    showToast("No route destination available for this aid point.");
    return;
  }
  window.open(getDirectionsUrl(place), "_blank", "noopener");
  showToast(`Opening route to ${place.name}`);
}

function renderAidList() {
  const filtered = getFilteredPlaces();
  $("#aidList").innerHTML = filtered.length ? filtered.map((place) => {
    const badgeText = place.source === "verified_responder" ? "Verified" : "Public Place";
    const placeId = escapeHtml(place.id);
    const placeName = escapeHtml(place.name);
    return `
      <article class="aid-item">
        <div class="aid-left">
          <span class="aid-icon" style="--cat:${escapeHtml(place.color)}">${escapeHtml(place.icon)}</span>
          <div class="aid-text">
            <strong>${placeName}</strong>
            <span>${escapeHtml(place.distance)} - ${escapeHtml(place.eta)} - ${escapeHtml(place.open)}</span>
            <span class="badge ${place.verified ? "verified" : "public"}">${badgeText}</span>
          </div>
        </div>
        <div class="aid-actions">
          <button class="circle-action" type="button" aria-label="Call ${placeName}" data-aid-call="${placeId}">Call</button>
          <button class="circle-action" type="button" aria-label="Route to ${placeName}" data-aid-route="${placeId}">Go</button>
        </div>
      </article>
    `;
  }).join("") : `
    <article class="aid-item">
      <div class="aid-left">
        <span class="aid-icon" style="--cat:#757575">!</span>
        <div class="aid-text">
          <strong>No matches in this filter</strong>
          <span>Try All or expand the search radius.</span>
        </div>
      </div>
    </article>
  `;
}

function setPanel(panel) {
  $$(".mode-tab").forEach((button) => button.classList.toggle("active", button.dataset.panel === panel));
  $$(".panel-view").forEach((view) => view.classList.remove("active"));
  $(`#${panel}Panel`).classList.add("active");
  $("#panelTitle").textContent = {
    console: "Emergency Console",
    nearby: "Nearby Aid",
    responder: "Responder Alert",
    settings: "Settings"
  }[panel];
}

function updateIncidentFromForm() {
  const type = $("#incidentType").value;
  const people = $("#peopleCount").value || "0";
  const location = $("#locationInput").value || "Current location";
  $("#incidentTitle").textContent = `${type} - ${people} ${people === "1" ? "person" : "people"}`;
  $("#incidentPlace").textContent = location;
  $("#alertType").textContent = type;
  $("#alertPeople").textContent = people;
  $("#alertHeadline").textContent = `New ${type}`;
}

async function refreshHealth() {
  if (!isServerMode()) {
    $("#providerLabel").textContent = "Direct file mode: local mock data";
    return;
  }

  try {
    const response = await fetch("/api/health");
    const data = await response.json();
    $("#providerLabel").textContent = `Backend maps provider: ${data.mapsProvider}`;
    $("#networkStatus").textContent = "API Online";
  } catch {
    $("#providerLabel").textContent = "Backend unavailable: local mock fallback";
    $("#networkStatus").textContent = "API Offline";
    $("#networkStatus").classList.add("offline");
  }
}

async function refreshNearbyAid(options = {}) {
  if (!isServerMode()) {
    places = [...mockPlaces];
    renderAidList();
    if (options.showMessage) showToast("Nearby aid refreshed from local mock data");
    return;
  }

  const incidentType = options.incidentType || categoryToIncident[state.selectedCategory] || $("#incidentType").value || "Accident";
  const params = getNearbyQueryParams(incidentType);

  try {
    const response = await fetch(`/api/nearby/aid?${params.toString()}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const data = await response.json();
    places = Array.isArray(data.results) ? data.results.map(normalizeApiPlace) : [...mockPlaces];
    state.apiProvider = data.provider || "backend";
    renderAidList();
    if (options.showMessage) showToast(`Nearby aid refreshed from ${state.apiProvider}`);
  } catch {
    places = [...mockPlaces];
    state.apiProvider = "local mock fallback";
    renderAidList();
    showToast("Backend unavailable. Using local mock nearby data.");
  }
}

async function createIncident() {
  updateIncidentFromForm();
  if (!isServerMode()) {
    state.lastAlertId = null;
    $("#alertId").textContent = "Local";
    return;
  }

  const payload = {
    type: $("#incidentType").value,
    peopleCount: $("#peopleCount").value,
    locationText: $("#locationInput").value,
    note: $("#incidentNote").value
  };

  const response = await fetch("/api/incidents", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(payload)
  });

  if (!response.ok) throw new Error(`Incident API failed: ${response.status}`);
  const data = await response.json();
  state.lastAlertId = data.alert?.id || null;
  $("#alertId").textContent = state.lastAlertId || "None";
}

async function decideResponder(decision) {
  if (!state.lastAlertId || !isServerMode()) return;
  await fetch(`/api/responder-alerts/${state.lastAlertId}/${decision}`, { method: "POST" });
}

function finishSosCountdown() {
  createIncident()
    .then(() => {
      setPanel("responder");
      $("#incidentStatus").textContent = state.offline ? "SMS SOS queued" : "Responders notified";
      $("#incidentEta").textContent = state.offline ? "Will sync when online" : "Waiting for acceptance";
      showToast(state.offline ? "Offline SOS queued via SMS fallback" : "Verified responders notified through API");
    })
    .catch(() => {
      setPanel("responder");
      $("#incidentStatus").textContent = "Local alert created";
      $("#incidentEta").textContent = "Backend sync failed";
      showToast("Backend unavailable. Incident kept in local prototype state.");
    });
}

function triggerSos() {
  const button = $("#sosButton");
  if (state.sosTimer) {
    window.clearInterval(state.sosTimer);
    state.sosTimer = null;
    state.sosRemaining = 0;
    button.textContent = "SOS";
    button.classList.remove("counting");
    showToast("SOS cancelled");
    return;
  }

  state.sosRemaining = 3;
  button.classList.add("counting");
  button.textContent = state.sosRemaining;
  showToast("SOS countdown started. Tap again to cancel.");
  state.sosTimer = window.setInterval(() => {
    state.sosRemaining -= 1;
    button.textContent = state.sosRemaining > 0 ? state.sosRemaining : "SOS";
    if (state.sosRemaining <= 0) {
      window.clearInterval(state.sosTimer);
      state.sosTimer = null;
      button.classList.remove("counting");
      finishSosCountdown();
    }
  }, 900);
}

function bindEvents() {
  $("#categoryGrid").addEventListener("click", (event) => {
    const button = event.target.closest("[data-category]");
    if (!button) return;
    state.selectedCategory = button.dataset.category;
    renderCategories();
    setPanel("nearby");
    refreshNearbyAid({ showMessage: true });
  });

  $$(".mode-tab").forEach((button) => {
    button.addEventListener("click", () => setPanel(button.dataset.panel));
  });

  $$(".nav-item, [data-tab='nearby']").forEach((button) => {
    button.addEventListener("click", () => {
      $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.tab === button.dataset.tab));
      if (button.dataset.tab === "nearby") {
        setPanel("nearby");
        refreshNearbyAid();
      }
      if (button.dataset.tab === "sos") setPanel("console");
      if (button.dataset.tab === "profile") setPanel("settings");
      if (button.dataset.tab === "incidents") setPanel("responder");
    });
  });

  $("#nearbyFilters").addEventListener("click", (event) => {
    const button = event.target.closest("[data-filter]");
    if (!button) return;
    state.nearbyFilter = button.dataset.filter;
    $$(".filter-chip").forEach((chip) => chip.classList.toggle("active", chip === button));
    renderAidList();
  });

  $("#sosButton").addEventListener("click", triggerSos);
  $("#locateButton").addEventListener("click", useCurrentLocation);

  $("#incidentForm").addEventListener("submit", (event) => {
    event.preventDefault();
    createIncident()
      .then(() => {
        setPanel("responder");
        $("#incidentStatus").textContent = "Responders notified";
        $("#incidentEta").textContent = "Waiting for acceptance";
        showToast("Incident created through backend API");
      })
      .catch(() => {
        setPanel("responder");
        $("#incidentStatus").textContent = "Local alert created";
        $("#incidentEta").textContent = "Backend sync failed";
        showToast("Backend unavailable. Incident kept in local prototype state.");
      });
  });

  $("#findAidButton").addEventListener("click", () => {
    updateIncidentFromForm();
    setPanel("nearby");
    refreshNearbyAid({ incidentType: $("#incidentType").value, showMessage: true });
  });

  $("#acceptButton").addEventListener("click", () => {
    decideResponder("accept").catch(() => null);
    $("#incidentStatus").textContent = "Responder en route";
    $("#incidentEta").textContent = "1.8 km away - ETA 4 min";
    showToast("Responder accepted and route is live");
  });

  $("#declineButton").addEventListener("click", () => {
    decideResponder("decline").catch(() => null);
    $("#incidentStatus").textContent = "Searching next responder";
    $("#incidentEta").textContent = "Fallback notification in progress";
    showToast("Responder declined. Matching next verified unit.");
  });

  $("#offlineToggle").addEventListener("change", (event) => {
    state.offline = event.target.checked;
    $("#networkStatus").textContent = state.offline ? "Offline" : "API Online";
    $("#networkStatus").classList.toggle("offline", state.offline);
    showToast(state.offline ? "Offline mode enabled: SMS SOS and cached map active" : "Online mode restored");
  });

  document.body.addEventListener("click", (event) => {
    const callButton = event.target.closest("[data-aid-call]");
    if (callButton) {
      const place = getPlaceById(callButton.dataset.aidCall);
      if (place) callAid(place);
      return;
    }

    const routeButton = event.target.closest("[data-aid-route]");
    if (routeButton) {
      const place = getPlaceById(routeButton.dataset.aidRoute);
      if (place) routeToAid(place);
      return;
    }

    const target = event.target.closest("[data-toast]");
    if (target) showToast(target.dataset.toast);
    if (event.target.closest("[data-action='call']")) {
      window.location.href = "tel:112";
      showToast("Opening official emergency call fallback");
    }
    if (event.target.closest("[data-action='share']")) showToast("Location share link prepared");
  });
}

renderCategories();
renderAidList();
bindEvents();
updateLocationUi();
refreshHealth();
refreshNearbyAid();
