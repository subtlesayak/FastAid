const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");

const { JsonStore } = require("./store");

test("persists incidents across store instances", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-store-"));
  const filePath = path.join(directory, "data.json");

  const first = new JsonStore(filePath);
  first.upsert("incidents", { id: "inc_1", status: "created" });

  const second = new JsonStore(filePath);
  assert.deepEqual(second.get("incidents", "inc_1"), {
    id: "inc_1",
    status: "created"
  });
});

test("seeds verified responders without duplicating them", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-store-"));
  const store = new JsonStore(path.join(directory, "data.json"));
  const responders = [{ id: "responder_1", verificationStatus: "verified" }];

  store.seedResponders(responders);
  store.seedResponders(responders);

  assert.equal(store.list("responders").length, 1);
});

test("rejects records without an id", () => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "fastaid-store-"));
  const store = new JsonStore(path.join(directory, "data.json"));

  assert.throws(
    () => store.upsert("alerts", { status: "notified" }),
    /requires a non-empty id/
  );
});
