function isHttpsUrl(value) {
  try {
    return new URL(value).protocol === "https:";
  } catch (error) {
    return false;
  }
}

function evaluatePilotReadiness(options = {}) {
  const mode = options.mode === "pilot" ? "pilot" : "local";
  const notificationProvider = options.notificationProvider === "firebase" ? "firebase" : "local";
  const checks = {
    storage: {
      required: true,
      ready: Boolean(options.storageReady),
      detail: options.storageReady ? "Pilot store is readable and writable" : "Pilot store is unavailable"
    },
    publicUrl: {
      required: mode === "pilot",
      ready: isHttpsUrl(options.publicBaseUrl),
      detail: isHttpsUrl(options.publicBaseUrl)
        ? "HTTPS public URL configured"
        : "Set FASTAID_PUBLIC_BASE_URL to the deployed HTTPS origin"
    },
    maps: {
      required: mode === "pilot",
      ready: Boolean(options.mapsKeyPresent),
      detail: options.mapsKeyPresent
        ? "Server-side Google Maps key configured"
        : "GOOGLE_MAPS_SERVER_KEY is not configured"
    },
    administrator: {
      required: mode === "pilot",
      ready: Boolean(options.adminConfigured),
      detail: options.adminConfigured
        ? "Active pilot administrator exists"
        : "Bootstrap an administrator with deployment secrets"
    },
    notifications: {
      required: mode === "pilot",
      ready: notificationProvider === "local" || Boolean(options.firebaseAdapterReady),
      detail: notificationProvider === "local"
        ? "Local simulated delivery adapter active"
        : (options.firebaseAdapterReady
          ? "Firebase delivery adapter active"
          : "Firebase provider selected but no sender is injected"),
      mode: notificationProvider === "local" ? "simulated" : "remote"
    }
  };
  const ready = Object.values(checks).every((check) => !check.required || check.ready);
  const warnings = [];
  if (notificationProvider === "local") {
    warnings.push("Responder notifications are simulated locally and do not reach remote devices.");
  }
  if (mode === "local") {
    warnings.push("Local mode does not require HTTPS, a Maps key, or an administrator.");
  }

  return { ready, mode, checks, warnings };
}

module.exports = { evaluatePilotReadiness, isHttpsUrl };
