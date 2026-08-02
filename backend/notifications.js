const crypto = require("node:crypto");

class LocalNotificationAdapter {
  constructor() {
    this.provider = "local";
  }

  async sendResponderAlert({ alert, responder }) {
    return {
      messageId: `local_${alert.id}_${responder.id}`,
      deliveredAt: new Date().toISOString()
    };
  }
}

class FirebaseNotificationAdapter {
  constructor({ sendMessage } = {}) {
    this.provider = "firebase";
    this.sendMessage = sendMessage;
  }

  async sendResponderAlert(payload) {
    if (typeof this.sendMessage !== "function") {
      throw new Error("Firebase notification sender is not configured");
    }
    return this.sendMessage(payload);
  }
}

function createNotificationAdapter({ provider = "local", firebaseSendMessage } = {}) {
  if (provider === "firebase") {
    return new FirebaseNotificationAdapter({ sendMessage: firebaseSendMessage });
  }
  return new LocalNotificationAdapter();
}

async function deliverWithRetry(adapter, payload, options = {}) {
  const configuredAttempts = Number(options.maxAttempts || 3);
  const maxAttempts = Math.max(1, Math.min(5, Number.isFinite(configuredAttempts) ? configuredAttempts : 3));
  const attempts = [];

  for (let attemptNumber = 1; attemptNumber <= maxAttempts; attemptNumber += 1) {
    const startedAt = new Date().toISOString();
    try {
      const result = await adapter.sendResponderAlert(payload);
      const attempt = {
        id: `notification_${crypto.randomUUID()}`,
        provider: adapter.provider,
        status: "delivered",
        attemptNumber,
        startedAt,
        messageId: result?.messageId || "",
        deliveredAt: result?.deliveredAt || new Date().toISOString()
      };
      attempts.push(attempt);
      if (options.onAttempt) await options.onAttempt(attempt);
      return { delivered: true, attempts, result };
    } catch (error) {
      const attempt = {
        id: `notification_${crypto.randomUUID()}`,
        provider: adapter.provider,
        status: "failed",
        attemptNumber,
        startedAt,
        failedAt: new Date().toISOString(),
        error: error.message
      };
      attempts.push(attempt);
      if (options.onAttempt) await options.onAttempt(attempt);
    }
  }

  return { delivered: false, attempts, result: null };
}

module.exports = {
  FirebaseNotificationAdapter,
  LocalNotificationAdapter,
  createNotificationAdapter,
  deliverWithRetry
};
