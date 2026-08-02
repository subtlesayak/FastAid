const crypto = require("node:crypto");
const { promisify } = require("node:util");

const scrypt = promisify(crypto.scrypt);
const PASSWORD_KEY_BYTES = 64;
const DEFAULT_SESSION_TTL_MS = 60 * 60 * 1000;

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function validatePassword(password) {
  if (typeof password !== "string" || password.length < 8) {
    throw new Error("Password must contain at least 8 characters");
  }
  if (password.length > 256) {
    throw new Error("Password must contain at most 256 characters");
  }
}

async function hashPassword(password) {
  validatePassword(password);
  const salt = crypto.randomBytes(16).toString("base64url");
  const derivedKey = await scrypt(password, salt, PASSWORD_KEY_BYTES);
  return {
    algorithm: "scrypt",
    salt,
    hash: Buffer.from(derivedKey).toString("base64url"),
    keyBytes: PASSWORD_KEY_BYTES
  };
}

async function verifyPassword(password, credential) {
  if (!credential || credential.algorithm !== "scrypt") return false;
  if (typeof password !== "string" || !credential.salt || !credential.hash) return false;
  const keyBytes = Number(credential.keyBytes || PASSWORD_KEY_BYTES);
  const actual = Buffer.from(await scrypt(password, credential.salt, keyBytes));
  const expected = Buffer.from(credential.hash, "base64url");
  return actual.length === expected.length && crypto.timingSafeEqual(actual, expected);
}

function hashSessionToken(token) {
  return crypto.createHash("sha256").update(token).digest("hex");
}

function createSession(store, userId, options = {}) {
  const ttlMs = Number(options.ttlMs || DEFAULT_SESSION_TTL_MS);
  const now = options.now ? new Date(options.now) : new Date();
  const token = crypto.randomBytes(32).toString("base64url");
  const session = {
    id: `session_${crypto.randomUUID()}`,
    userId,
    tokenHash: hashSessionToken(token),
    createdAt: now.toISOString(),
    expiresAt: new Date(now.getTime() + ttlMs).toISOString()
  };
  store.upsert("sessions", session);
  return { token, session };
}

function readBearerToken(req) {
  const authorization = String(req.headers.authorization || "");
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  return match ? match[1].trim() : "";
}

function authenticateRequest(req, store, options = {}) {
  const token = readBearerToken(req);
  if (!token) return null;
  const tokenHash = hashSessionToken(token);
  const session = store.list("sessions").find((entry) => entry.tokenHash === tokenHash);
  if (!session) return null;

  const now = options.now ? new Date(options.now) : new Date();
  if (!session.expiresAt || new Date(session.expiresAt).getTime() <= now.getTime()) {
    store.remove("sessions", session.id);
    return null;
  }

  const user = store.get("users", session.userId);
  if (!user || user.status !== "active") return null;
  return { user, session };
}

function sanitizeUser(user) {
  if (!user) return null;
  const { passwordCredential, ...safeUser } = user;
  return safeUser;
}

module.exports = {
  DEFAULT_SESSION_TTL_MS,
  authenticateRequest,
  createSession,
  hashPassword,
  hashSessionToken,
  normalizeEmail,
  sanitizeUser,
  verifyPassword
};
