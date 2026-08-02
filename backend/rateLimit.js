class FixedWindowRateLimiter {
  constructor({ limit, windowMs, maxKeys = 10_000 }) {
    const parsedLimit = Number(limit);
    const parsedWindowMs = Number(windowMs);
    const parsedMaxKeys = Number(maxKeys);
    this.limit = Math.max(1, Number.isFinite(parsedLimit) ? parsedLimit : 10);
    this.windowMs = Math.max(1_000, Number.isFinite(parsedWindowMs) ? parsedWindowMs : 60_000);
    this.maxKeys = Math.max(100, Number.isFinite(parsedMaxKeys) ? parsedMaxKeys : 10_000);
    this.entries = new Map();
  }

  consume(key, now = Date.now()) {
    const normalizedKey = String(key || "unknown");
    let entry = this.entries.get(normalizedKey);
    if (!entry || entry.resetAt <= now) {
      entry = { count: 0, resetAt: now + this.windowMs };
    }
    entry.count += 1;
    this.entries.set(normalizedKey, entry);
    if (this.entries.size > this.maxKeys) this.#prune(now);

    return {
      allowed: entry.count <= this.limit,
      remaining: Math.max(0, this.limit - entry.count),
      retryAfterSeconds: Math.max(1, Math.ceil((entry.resetAt - now) / 1000))
    };
  }

  #prune(now) {
    for (const [key, entry] of this.entries) {
      if (entry.resetAt <= now) this.entries.delete(key);
    }
    if (this.entries.size <= this.maxKeys) return;
    const overflow = this.entries.size - this.maxKeys;
    for (const key of this.entries.keys()) {
      this.entries.delete(key);
      if (this.entries.size <= this.maxKeys - overflow) break;
    }
  }
}

module.exports = { FixedWindowRateLimiter };
