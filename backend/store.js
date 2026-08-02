const fs = require("node:fs");
const path = require("node:path");

const EMPTY_STATE = Object.freeze({
  incidents: [],
  alerts: [],
  responders: [],
  users: [],
  sessions: [],
  audits: [],
  notifications: []
});

class JsonStore {
  constructor(filePath) {
    this.filePath = path.resolve(filePath);
    this.state = this.#load();
  }

  list(collection) {
    this.#assertCollection(collection);
    return this.state[collection].map(clone);
  }

  get(collection, id) {
    this.#assertCollection(collection);
    const item = this.state[collection].find((entry) => entry.id === id);
    return item ? clone(item) : null;
  }

  upsert(collection, item) {
    this.#assertCollection(collection);
    if (collection === "audits") {
      throw new Error("Audit records are append-only");
    }
    if (!item || typeof item.id !== "string" || !item.id.trim()) {
      throw new Error(`Stored ${collection} item requires a non-empty id`);
    }

    const next = clone(item);
    const index = this.state[collection].findIndex((entry) => entry.id === next.id);
    if (index >= 0) this.state[collection][index] = next;
    else this.state[collection].push(next);
    this.#persist();
    return clone(next);
  }

  append(collection, item) {
    this.#assertCollection(collection);
    if (!item || typeof item.id !== "string" || !item.id.trim()) {
      throw new Error(`Stored ${collection} item requires a non-empty id`);
    }
    if (this.state[collection].some((entry) => entry.id === item.id)) {
      throw new Error(`Stored ${collection} item id already exists`);
    }
    this.state[collection].push(clone(item));
    this.#persist();
    return clone(item);
  }

  remove(collection, id) {
    this.#assertCollection(collection);
    if (collection === "audits") {
      throw new Error("Audit records are append-only");
    }
    const index = this.state[collection].findIndex((entry) => entry.id === id);
    if (index < 0) return false;
    this.state[collection].splice(index, 1);
    this.#persist();
    return true;
  }

  seedResponders(responders) {
    let changed = false;
    for (const responder of responders) {
      if (this.state.responders.some((entry) => entry.id === responder.id)) continue;
      this.state.responders.push(clone(responder));
      changed = true;
    }
    if (changed) this.#persist();
  }

  #load() {
    if (!fs.existsSync(this.filePath)) return clone(EMPTY_STATE);
    try {
      const parsed = JSON.parse(fs.readFileSync(this.filePath, "utf8"));
      return {
        incidents: Array.isArray(parsed.incidents) ? parsed.incidents : [],
        alerts: Array.isArray(parsed.alerts) ? parsed.alerts : [],
        responders: Array.isArray(parsed.responders) ? parsed.responders : [],
        users: Array.isArray(parsed.users) ? parsed.users : [],
        sessions: Array.isArray(parsed.sessions) ? parsed.sessions : [],
        audits: Array.isArray(parsed.audits) ? parsed.audits : [],
        notifications: Array.isArray(parsed.notifications) ? parsed.notifications : []
      };
    } catch (error) {
      throw new Error(`Unable to load FastAid data store: ${error.message}`);
    }
  }

  #persist() {
    fs.mkdirSync(path.dirname(this.filePath), { recursive: true });
    const temporaryPath = `${this.filePath}.tmp`;
    fs.writeFileSync(temporaryPath, `${JSON.stringify(this.state, null, 2)}\n`, "utf8");
    fs.renameSync(temporaryPath, this.filePath);
  }

  #assertCollection(collection) {
    if (!Object.prototype.hasOwnProperty.call(EMPTY_STATE, collection)) {
      throw new Error(`Unknown FastAid collection: ${collection}`);
    }
  }
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

module.exports = { JsonStore };
