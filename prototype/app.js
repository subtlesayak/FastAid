const categories = [
  { id: "accident", label: "Accident", icon: "✚", color: "#f03535", types: ["hospital", "police"] },
  { id: "breakdown", label: "Breakdown", icon: "◆", color: "#fb8c00", types: ["car_repair", "tire_shop"] },
  { id: "fuel", label: "Fuel", icon: "⛽", color: "#25a645", types: ["gas_station"] },
  { id: "medical", label: "Medical", icon: "✚", color: "#147fd7", types: ["hospital", "pharmacy"] },
  { id: "police", label: "Police", icon: "◆", color: "#147fd7", types: ["police"] },
  { id: "fire", label: "Fire", icon: "●", color: "#f03535", types: ["fire_station"] },
  { id: "repair", label: "Repair", icon: "⚙", color: "#25a645", types: ["car_repair"] },
  { id: "ev", label: "EV", icon: "↯", color: "#25a645", types: ["electric_vehicle_charging_station"] }
];

const places = [
  { id: 1, name: "City Hospital", type: "hospital", icon: "✚", distance: "1.2 km", eta: "5 min", open: "Open 24 hrs", verified: true, color: "#147fd7" },
  { id: 2, name: "Connaught Place Police Station", type: "police", icon: "◆", distance: "0.8 km", eta: "4 min", open: "Open", verified: true, color: "#147fd7" },
  { id: 3, name: "Speedy Auto Care", type: "car_repair", icon: "⚙", distance: "0.6 km", eta: "3 min", open: "Open", verified: false, color: "#25a645" },
  { id: 4, name: "HP Petrol Pump", type: "gas_station", icon: "⛽", distance: "1.0 km", eta: "4 min", open: "Open 24 hrs", verified: false, color: "#25a645" },
  { id: 5, name: "Tyre World", type: "tire_shop", icon: "◌", distance: "1.4 km", eta: "7 min", open: "Open", verified: false, color: "#25a645" },
  { id: 6, name: "HealthPlus Pharmacy", type: "pharmacy", icon: "✚", distance: "1.1 km", eta: "5 min", open: "Open", verified: false, color: "#f03535" },
  { id: 7, name: "Central Fire Station", type: "fire_station", icon: "●", distance: "2.2 km", eta: "8 min", open: "Open", verified: true, color: "#f03535" },
  { id: 8, name: "ChargeGrid EV Point", type: "electric_vehicle_charging_station", icon: "↯", distance: "1.7 km", eta: "6 min", open: "Open", verified: false, color: "#25a645" }
];

const state = {
  selectedCategory: "accident",
  nearbyFilter: "all",
  activePanel: "console",
  sosTimer: null,
  sosRemaining: 0,
  offline: false
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function renderCategories() {
  const grid = $("#categoryGrid");
  grid.innerHTML = categories.map((category) => `
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

function renderAidList() {
  const list = $("#aidList");
  const filtered = getFilteredPlaces();
  list.innerHTML = filtered.length ? filtered.map((place) => `
    <article class="aid-item">
      <div class="aid-left">
        <span class="aid-icon" style="--cat:${place.color}">${place.icon}</span>
        <div class="aid-text">
          <strong>${place.name}</strong>
          <span>${place.distance} • ${place.eta} • ${place.open}</span>
          <span class="badge ${place.verified ? "verified" : "public"}">${place.verified ? "Verified" : "Public Place"}</span>
        </div>
      </div>
      <div class="aid-actions">
        <button class="circle-action" type="button" aria-label="Call ${place.name}" data-toast="Calling ${place.name}">☎</button>
        <button class="circle-action" type="button" aria-label="Navigate to ${place.name}" data-toast="Opening route to ${place.name}">➤</button>
      </div>
    </article>
  `).join("") : `
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
  state.activePanel = panel;
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

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => toast.classList.remove("show"), 2400);
}

function updateIncidentFromForm() {
  const type = $("#incidentType").value;
  const people = $("#peopleCount").value || "0";
  const location = $("#locationInput").value || "Current location";
  $("#incidentTitle").textContent = `${type} • ${people} ${people === "1" ? "person" : "people"}`;
  $("#incidentPlace").textContent = location;
  $("#alertType").textContent = type;
  $("#alertPeople").textContent = people;
  $("#alertHeadline").textContent = `New ${type}`;
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
      updateIncidentFromForm();
      setPanel("responder");
      $("#incidentStatus").textContent = state.offline ? "SMS SOS queued" : "Responders notified";
      $("#incidentEta").textContent = state.offline ? "Will sync when online" : "Waiting for acceptance";
      showToast(state.offline ? "Offline SOS queued via SMS fallback" : "Verified responders notified");
    }
  }, 900);
}

function bindEvents() {
  $("#categoryGrid").addEventListener("click", (event) => {
    const button = event.target.closest("[data-category]");
    if (!button) return;
    state.selectedCategory = button.dataset.category;
    renderCategories();
    renderAidList();
    setPanel("nearby");
  });

  $$(".mode-tab").forEach((button) => {
    button.addEventListener("click", () => setPanel(button.dataset.panel));
  });

  $$(".nav-item, [data-tab='nearby']").forEach((button) => {
    button.addEventListener("click", () => {
      $$(".nav-item").forEach((item) => item.classList.toggle("active", item.dataset.tab === button.dataset.tab));
      if (button.dataset.tab === "nearby") setPanel("nearby");
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

  $("#incidentForm").addEventListener("submit", (event) => {
    event.preventDefault();
    updateIncidentFromForm();
    setPanel("responder");
    $("#incidentStatus").textContent = "Responders notified";
    $("#incidentEta").textContent = "Waiting for acceptance";
    showToast("Incident created and sent to verified responders");
  });

  $("#findAidButton").addEventListener("click", () => {
    updateIncidentFromForm();
    setPanel("nearby");
    showToast("Nearby aid refreshed from mock provider");
  });

  $("#acceptButton").addEventListener("click", () => {
    $("#incidentStatus").textContent = "Responder en route";
    $("#incidentEta").textContent = "1.8 km away • ETA 4 min";
    showToast("Responder accepted and route is live");
  });

  $("#declineButton").addEventListener("click", () => {
    $("#incidentStatus").textContent = "Searching next responder";
    $("#incidentEta").textContent = "Fallback notification in progress";
    showToast("Responder declined. Matching next verified unit.");
  });

  $("#offlineToggle").addEventListener("change", (event) => {
    state.offline = event.target.checked;
    $("#networkStatus").textContent = state.offline ? "Offline" : "Online";
    $("#networkStatus").classList.toggle("offline", state.offline);
    showToast(state.offline ? "Offline mode enabled: SMS SOS and cached map active" : "Online mode restored");
  });

  document.body.addEventListener("click", (event) => {
    const target = event.target.closest("[data-toast]");
    if (target) showToast(target.dataset.toast);
    if (event.target.closest("[data-action='call']")) showToast("Opening official emergency call fallback");
    if (event.target.closest("[data-action='share']")) showToast("Location share link prepared");
  });
}

renderCategories();
renderAidList();
bindEvents();
