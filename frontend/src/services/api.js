const API_BASE = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE || "http://localhost:8080";

export function getToken() {
  return localStorage.getItem("ss_token");
}

export function setAuth(token, email = null, name = null) {
  localStorage.setItem("ss_token", token);
  if (email) localStorage.setItem("ss_email", email);
  if (name) localStorage.setItem("ss_name", name);
  window.dispatchEvent(new Event("ss-auth-changed"));
}

export function logout() {
  localStorage.removeItem("ss_token");
  localStorage.removeItem("ss_email");
  localStorage.removeItem("ss_name");
  window.dispatchEvent(new Event("ss-auth-changed"));
}

function tokenPayload(token) {
  try {
    const encodedPayload = token.split(".")[1];
    const base64 = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), "=");
    return JSON.parse(atob(padded));
  } catch (_) {
    return null;
  }
}

function tokenIsExpired(token) {
  const payload = tokenPayload(token);
  return typeof payload?.exp === "number" && payload.exp * 1000 <= Date.now();
}

export function isLoggedIn() {
  const token = getToken();
  if (!token) return false;
  if (tokenIsExpired(token)) {
    logout();
    return false;
  }
  return true;
}

export function getUser() {
  const tokenUser = tokenPayload(getToken() || "");
  const pendingProfile = readPendingProfile();
  const storedEmail = localStorage.getItem("ss_email") || tokenUser?.email || tokenUser?.sub || "";
  return {
    email: storedEmail,
    name:
      localStorage.getItem("ss_name") ||
      tokenUser?.name ||
      tokenUser?.firstName ||
      tokenUser?.fullName ||
      (pendingProfile?.email === storedEmail ? pendingProfile.name : "") ||
      ""
  };
}

function readPendingProfile() {
  try {
    return JSON.parse(localStorage.getItem("ss_pending_profile") || "null");
  } catch (_) {
    return null;
  }
}

export function rememberProfile(email, name) {
  localStorage.setItem("ss_pending_profile", JSON.stringify({
    email: email.trim().toLowerCase(),
    name: name.trim()
  }));
}

function authHeaders() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseError(res) {
  let data = null;
  try {
    data = await res.json();
  } catch (_) {
    // Non-JSON error response.
  }

  return data?.message || data?.error || `Request failed (${res.status})`;
}

export async function api(path, options = {}) {
  const headers = {
    ...(options.headers || {}),
    ...authHeaders()
  };

  const res = await fetch(API_BASE + path, {
    ...options,
    headers
  });

  if (!res.ok) {
    if (res.status === 401 && getToken()) logout();
    const error = new Error(await parseError(res));
    error.status = res.status;
    throw error;
  }

  if (res.status === 204) return null;

  const type = res.headers.get("content-type") || "";
  return type.includes("application/json") ? res.json() : res.text();
}

export async function login(email, password) {
  const data = await api("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password })
  });

  const account = data.user || data.account || data;
  const pendingProfile = readPendingProfile();
  const normalizedEmail = email.trim().toLowerCase();
  setAuth(
    data.token,
    account.email || normalizedEmail,
    account.name || account.firstName || account.fullName || account.username ||
      (pendingProfile?.email === normalizedEmail ? pendingProfile.name : null)
  );
  return data;
}

export async function register(data) {
  return api("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data)
  });
}

export async function requestRegistrationOtp(data) {
  return register(data);
}

export async function verifyRegistrationOtp(email, otp) {
  return api("/api/auth/register/verify-otp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, otp })
  });
}

export async function resendRegistrationOtp(data) {
  return api("/api/auth/register/resend-otp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data)
  });
}

export async function forgotPassword(email) {
  return api("/api/auth/forgot-password", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email })
  });
}

export async function resendForgotOtp(email) {
  return api("/api/auth/forgot-password/resend-otp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email })
  });
}

export async function verifyForgotOtp(email, otp) {
  return api("/api/auth/forgot-password/verify-otp", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, otp })
  });
}

export async function resetPassword(email, password, confirmPassword) {
  return api("/api/auth/forgot-password/reset", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, confirmPassword })
  });
}

export async function createQuickShare(file, expiryHours) {
  const form = new FormData();
  form.append("file", file);
  form.append("expiryHours", String(expiryHours));

  return api("/api/files/quick-share", {
    method: "POST",
    body: form
  });
}

export async function createSecureShare(file, expiryHours, maxDownloads, emails) {
  const form = new FormData();
  form.append("file", file);
  form.append("expiryHours", String(expiryHours));
  form.append("maxDownloads", String(maxDownloads));

  emails.forEach((item) => {
    form.append("allowedUserEmails", item);
  });

  return api("/api/files/share", {
    method: "POST",
    body: form
  });
}

export async function updateExpiry(token, hours) {
  return api(
    `/api/files/share/${encodeURIComponent(token)}/expiry?expiryHours=${encodeURIComponent(hours)}`,
    { method: "POST" }
  );
}

export async function revokeShare(token) {
  return api(
    `/api/files/share/${encodeURIComponent(token)}/revoke`,
    { method: "POST" }
  );
}

export async function getShareHistory() {
  return api("/api/files/shares/history");
}

export async function getDownloadHistory() {
  return api("/api/files/download-history");
}

// -----------------------------------------------------
// ONE DOWNLOAD API FOR BOTH QUICK + SECURE SHARES
// -----------------------------------------------------

export async function downloadBlob(token) {
  const res = await fetch(
    `${API_BASE}/api/files/download/${encodeURIComponent(token)}`,
    { headers: authHeaders() }
  );

  if (!res.ok) {
    if (res.status === 401 && getToken()) logout();
    const error = new Error(await parseError(res));
    error.status = res.status;
    throw error;
  }

  const blob = await res.blob();
  const disposition = res.headers.get("Content-Disposition") || "";
  const match = disposition.match(/filename="([^"]+)"/i);

  return {
    blob,
    filename: match?.[1] || "download"
  };
}

// This is the USER-SHAREABLE URL.
// The user opens the React route first; React captures the token,
// returns to Home, and Home talks to the backend download endpoint.
export function downloadUrl(token) {
  return `${window.location.origin}/download/${encodeURIComponent(token)}`;
}

// Extract a token from either the new frontend share URL or the old
// backend URL. This keeps old copied links usable while the new links
// are generated as /download/{token}.
export function extractShareToken(value) {
  try {
    const url = new URL(value, window.location.origin);
    const parts = url.pathname.split("/").filter(Boolean);

    const downloadIndex = parts.indexOf("download");
    if (downloadIndex >= 0 && parts[downloadIndex + 1]) {
      return decodeURIComponent(parts[downloadIndex + 1]);
    }

    const filesIndex = parts.indexOf("files");
    if (
      filesIndex >= 0 &&
      parts[filesIndex + 1] === "download" &&
      parts[filesIndex + 2]
    ) {
      return decodeURIComponent(parts[filesIndex + 2]);
    }
  } catch (_) {
    // Invalid URL.
  }

  return null;
}
