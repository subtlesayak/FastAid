const assert = require("node:assert/strict");
const test = require("node:test");

const { FixedWindowRateLimiter } = require("./rateLimit");

test("fixed-window limiter blocks excess requests and resets after the window", () => {
  const limiter = new FixedWindowRateLimiter({ limit: 2, windowMs: 1_000 });

  assert.equal(limiter.consume("client", 0).allowed, true);
  assert.equal(limiter.consume("client", 100).allowed, true);
  const blocked = limiter.consume("client", 200);
  assert.equal(blocked.allowed, false);
  assert.equal(blocked.remaining, 0);
  assert.equal(limiter.consume("client", 1_001).allowed, true);
});

test("fixed-window limiter isolates clients", () => {
  const limiter = new FixedWindowRateLimiter({ limit: 1, windowMs: 1_000 });

  assert.equal(limiter.consume("client-a", 0).allowed, true);
  assert.equal(limiter.consume("client-a", 1).allowed, false);
  assert.equal(limiter.consume("client-b", 1).allowed, true);
});
