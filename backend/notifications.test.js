const assert = require("node:assert/strict");
const test = require("node:test");

const {
  FirebaseNotificationAdapter,
  LocalNotificationAdapter,
  deliverWithRetry
} = require("./notifications");

test("local notification adapter delivers deterministic alert IDs", async () => {
  const adapter = new LocalNotificationAdapter();
  const delivery = await deliverWithRetry(adapter, {
    alert: { id: "alert_1" },
    responder: { id: "responder_1" }
  });

  assert.equal(delivery.delivered, true);
  assert.equal(delivery.attempts.length, 1);
  assert.equal(delivery.attempts[0].provider, "local");
  assert.equal(delivery.attempts[0].messageId, "local_alert_1_responder_1");
});

test("delivery retries a Firebase-compatible sender and records every attempt", async () => {
  let calls = 0;
  const adapter = new FirebaseNotificationAdapter({
    sendMessage: async () => {
      calls += 1;
      if (calls < 3) throw new Error(`temporary-${calls}`);
      return { messageId: "fcm_message_1" };
    }
  });
  const recorded = [];

  const delivery = await deliverWithRetry(adapter, {}, {
    maxAttempts: 3,
    onAttempt: (attempt) => recorded.push(attempt)
  });

  assert.equal(delivery.delivered, true);
  assert.equal(calls, 3);
  assert.deepEqual(recorded.map((attempt) => attempt.status), ["failed", "failed", "delivered"]);
  assert.equal(recorded[2].messageId, "fcm_message_1");
});

test("delivery stops at the configured retry bound", async () => {
  const adapter = new FirebaseNotificationAdapter();
  const delivery = await deliverWithRetry(adapter, {}, { maxAttempts: 2 });

  assert.equal(delivery.delivered, false);
  assert.equal(delivery.attempts.length, 2);
  assert.ok(delivery.attempts.every((attempt) => attempt.status === "failed"));
});
