import React, { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSecureShareDraft } from "../context/SecureShareDraftContext";
import {
  createQuickShare,
  createSecureShare,
  isLoggedIn,
  downloadBlob,
  downloadUrl,
  extractShareToken
} from "../services/api";

function Result({ result }) {
  const [copied, setCopied] = useState(false);
  if (!result) return null;

  async function copyLink() {
    try {
      await navigator.clipboard.writeText(result.url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1400);
    } catch (_) {
      setCopied(false);
    }
  }

  return (
    <div className="result-card">
      <div className="result-head">
        <span className="check">✓</span>
        <div>
          <b>{result.quick ? "Quick Share created" : "Secure Share created"}</b>
          <small>Your share link is ready.</small>
        </div>
      </div>

      <div className="share-link">
        <input readOnly value={result.url} />
        <button className="btn primary" onClick={copyLink}>
          {copied ? "Copied" : "Copy"}
        </button>
      </div>

      <div className="otp-note">
        {result.quick
          ? "No login is required to download this Quick Share."
          : "Secure Share checks login and authorization automatically."}
      </div>

      <button
        className="text-btn"
        onClick={() => window.open(result.url, "_blank", "noopener,noreferrer")}
      >
        Open share link →
      </button>
    </div>
  );
}

function startBlobDownload(result) {
  const objectUrl = URL.createObjectURL(result.blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = result.filename || "download";
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}

function isLoginRequired(error) {
  const message = String(error?.message || "").toLowerCase();
  return (
    error?.status === 401 ||
    message.includes("login is required") ||
    message.includes("login required") ||
    message.includes("authentication required") ||
    message.includes("please login") ||
    message.includes("please log in")
  );
}

function isTerminalShareError(error) {
  const message = String(error?.message || "").toLowerCase();
  return (
    message.includes("not allowed") ||
    message.includes("not authorized") ||
    message.includes("unauthorized") ||
    message.includes("expired") ||
    message.includes("revoked") ||
    message.includes("download limit") ||
    message.includes("share link not found") ||
    message.includes("not found")
  );
}

function validWholeNumber(value, min, max) {
  return /^\d+$/.test(String(value)) && Number(value) >= min && Number(value) <= max;
}

export default function Home() {
  const navigate = useNavigate();
  const quickInputRef = useRef(null);
  const secureInputRef = useRef(null);
  const autoDownloadStarted = useRef(false);

  const [quickFile, setQuickFile] = useState(null);
  const [quickExp, setQuickExp] = useState("24");
  const [quickBusy, setQuickBusy] = useState(false);
  const [quickRes, setQuickRes] = useState(null);
  const [quickDrag, setQuickDrag] = useState(false);

  const {
    secureFile,
    setSecureFile,
    secureExp,
    setSecureExp,
    max,
    setMax,
    restrict,
    setRestrict,
    emails,
    setEmails
  } = useSecureShareDraft();

  const [email, setEmail] = useState("");
  const [secureBusy, setSecureBusy] = useState(false);
  const [secureRes, setSecureRes] = useState(null);
  const [secureDrag, setSecureDrag] = useState(false);

  const [link, setLink] = useState("");
  const [downloadBusy, setDownloadBusy] = useState(false);
  const [downloadMsg, setDownloadMsg] = useState("");
  const [downloadError, setDownloadError] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (autoDownloadStarted.current) return;
    const token = sessionStorage.getItem("ss_pending_download_token");
    if (!token) return;

    autoDownloadStarted.current = true;
    processDownloadToken(token, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    // Login/Register returns to /#secure. Wait one frame so the restored
    // form is mounted, then bring the user back to the exact creation area.
    if (window.location.hash === "#secure") {
      requestAnimationFrame(() => {
        document.getElementById("secure")?.scrollIntoView({
          behavior: "smooth",
          block: "center"
        });
      });
    }
  }, []);

  function pickFile(file, type) {
    if (!file) return;
    if (type === "quick") setQuickFile(file);
    else setSecureFile(file);
    setError("");
  }

  function dropHandlers(type) {
    return {
      onDragEnter: (event) => {
        event.preventDefault();
        event.stopPropagation();
        type === "quick" ? setQuickDrag(true) : setSecureDrag(true);
      },
      onDragOver: (event) => {
        event.preventDefault();
        event.stopPropagation();
        event.dataTransfer.dropEffect = "copy";
      },
      onDragLeave: (event) => {
        event.preventDefault();
        event.stopPropagation();
        if (event.currentTarget === event.target) {
          type === "quick" ? setQuickDrag(false) : setSecureDrag(false);
        }
      },
      onDrop: (event) => {
        event.preventDefault();
        event.stopPropagation();
        type === "quick" ? setQuickDrag(false) : setSecureDrag(false);
        pickFile(event.dataTransfer.files?.[0], type);
      }
    };
  }

  function addEmail() {
    const value = email.trim().toLowerCase();
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!value) return setError("Enter an email address.");
    if (!emailPattern.test(value)) return setError("Enter a valid email address.");
    if (emails.includes(value)) return setError("This email is already added.");

    setEmails((current) => {
      const updatedEmails = [...current, value];

      // At least one total download should be possible for every
      // explicitly authorized user. Never reduce a value the user set.
      setMax((currentMax) => {
        const currentValue = Number(currentMax);
        return !Number.isFinite(currentValue) || currentValue < updatedEmails.length
          ? String(updatedEmails.length)
          : currentMax;
      });

      return updatedEmails;
    });

    setEmail("");
    setError("");
  }

  function removeEmail(value) {
    setEmails((current) => current.filter((item) => item !== value));
  }

  function clearQuickFile() {
    setQuickFile(null);
    if (quickInputRef.current) quickInputRef.current.value = "";
  }

  function clearSecureFile() {
    setSecureFile(null);
    if (secureInputRef.current) secureInputRef.current.value = "";
  }

  async function runQuick() {
    if (!quickFile) return setError("Please select a file.");
    if (!validWholeNumber(quickExp, 1, 24)) {
      return setError("Quick Share expiry must be between 1 and 24 hours.");
    }

    setQuickBusy(true);
    setError("");

    try {
      const response = await createQuickShare(quickFile, Number(quickExp));
      setQuickRes({
        quick: true,
        url: downloadUrl(response.shareToken),
        token: response.shareToken
      });
      clearQuickFile();
    } catch (e) {
      setError(e.message || "Unable to create Quick Share.");
    } finally {
      setQuickBusy(false);
    }
  }

  async function runSecure() {
    if (!isLoggedIn()) {
      // Keep the complete Secure Share draft in React/browser memory.
      // The File object is intentionally NOT uploaded just for login.
      // It remains available while this SPA tab stays open.
      sessionStorage.setItem("ss_after_login", "/#secure");
      navigate("/login");
      return;
    }

    if (!secureFile) return setError("Please select a file.");
    if (!validWholeNumber(secureExp, 1, 720)) {
      return setError("Secure Share expiry must be between 1 and 720 hours.");
    }
    if (max === "" || !validWholeNumber(max, 1, Number.MAX_SAFE_INTEGER)) {
      return setError("Maximum downloads must be at least 1.");
    }
    if (restrict && emails.length === 0) {
      return setError("Add at least one authorized email address.");
    }

    if (restrict && Number(max) < emails.length) {
      return setError(
        `Maximum downloads cannot be less than the number of authorized users (${emails.length}).`
      );
    }

    setSecureBusy(true);
    setError("");

    try {
      const response = await createSecureShare(
        secureFile,
        Number(secureExp),
        Number(max),
        restrict ? emails : []
      );

      setSecureRes({
        quick: false,
        url: downloadUrl(response.shareToken),
        token: response.shareToken
      });
      clearSecureFile();
    } catch (e) {
      setError(e.message || "Unable to create Secure Share.");
    } finally {
      setSecureBusy(false);
    }
  }

  async function processDownloadToken(token) {
    if (!token) {
      setDownloadError(true);
      setDownloadMsg("Invalid share link.");
      return;
    }

    setDownloadBusy(true);
    setDownloadError(false);
    setDownloadMsg("Checking your share link...");

    try {
      const result = await downloadBlob(token);
      startBlobDownload(result);
      sessionStorage.removeItem("ss_pending_download_token");
      setDownloadMsg(`Download started: ${result.filename}`);
    } catch (e) {
      if (isLoginRequired(e) && !isLoggedIn()) {
        sessionStorage.setItem("ss_pending_download_token", token);
        navigate("/login");
        return;
      }

      if (isTerminalShareError(e)) {
        sessionStorage.removeItem("ss_pending_download_token");
      }

      setDownloadMsg(e.message || "This share cannot be downloaded.");
      setDownloadError(true);
    } finally {
      setDownloadBusy(false);
    }
  }

  async function runDownload() {
    const raw = link.trim();
    if (!raw) {
      setDownloadError(true);
      setDownloadMsg("Paste a share link first.");
      return;
    }

    const token = extractShareToken(raw);
    if (!token) {
      setDownloadError(true);
      setDownloadMsg("Invalid SecureShare link.");
      return;
    }

    sessionStorage.setItem("ss_pending_download_token", token);
    await processDownloadToken(token);
  }

  function goToDownloadSection() {
    document.getElementById("download")?.scrollIntoView({
      behavior: "smooth",
      block: "center"
    });
  }

  return (
    <main className="home-page">
      <section className="hero">
        <div className="hero-copy">
          <div className="kicker">SECURE FILE SHARING, MADE SIMPLE</div>
          <h1>Send it.<br /><span>Secure it.</span></h1>
          <p>Quick links for convenience. Secure shares when access needs to be controlled.</p>
          <div className="hero-visual" aria-hidden="true">
            <span className="live-orbit orbit-one" />
            <span className="live-orbit orbit-two" />
            <img src="/File_Sharing.png" alt="" />
            <span className="live-status"><i /> Live sharing flow</span>
          </div>
        </div>

        <div className="download-card" id="download">
          <div className="card-icon">↗</div>
          <div>
            <h2>Have a share link?</h2>
            <p>Paste your link below to download the file.</p>
          </div>

          <div className="download-row">
            <input
              value={link}
              onChange={(e) => setLink(e.target.value)}
              placeholder="Paste your SecureShare link..."
            />
            <button className="btn primary download-btn" onClick={runDownload} disabled={downloadBusy}>
              {downloadBusy ? "Checking..." : "Download ↓"}
            </button>
          </div>

          <small>🔒 Quick Shares work without login. Secure Shares automatically check login and authorization.</small>

          {downloadMsg && (
            <div className={`inline-msg ${downloadError ? "error" : "success"}`}>
              {downloadMsg}
              {downloadError && isLoggedIn() && (
                <button className="inline-action" onClick={goToDownloadSection}>Try again</button>
              )}
            </div>
          )}
        </div>
      </section>

      <section className="cards">
        <div className="share-card quick-card" id="quick">
          <div className="kicker">⚡ QUICK SHARE</div>
          <h2>Send a file in seconds</h2>
          <p>No login required. Create a quick link with an expiry of 1–24 hours.</p>

          <label
            className={`file-picker ${quickDrag ? "dragging" : ""}`}
            {...dropHandlers("quick")}
          >
            <input
              ref={quickInputRef}
              type="file"
              onChange={(e) => pickFile(e.target.files?.[0], "quick")}
            />
            <span>↑ <b>{quickFile ? quickFile.name : "Select a file"}</b> or drop it here</span>
          </label>

          <div className="inline-fields">
            <label>
              Expires (hours)
              <input
                type="number"
                min="1"
                max="24"
                step="1"
                value={quickExp}
                onChange={(e) => setQuickExp(e.target.value)}
                onBlur={() => {
                  if (quickExp !== "" && !validWholeNumber(quickExp, 1, 24)) {
                    setError("Quick Share expiry must be between 1 and 24 hours.");
                  }
                }}
              />
            </label>
            <button className="btn primary" onClick={runQuick} disabled={quickBusy}>
              {quickBusy ? "Creating..." : "Create Quick Link →"}
            </button>
          </div>

          <Result result={quickRes} />
        </div>

        <div className="share-card secure-card" id="secure">
          <div className="kicker">🔐 CONTROLLED ACCESS</div>
          <h2>Secure Share</h2>
          <p>Share with <b>one or many email addresses</b>. Only the users you authorize can download when the access limit is enabled.</p>

          <label
            className={`file-picker ${secureDrag ? "dragging" : ""}`}
            {...dropHandlers("secure")}
          >
            <input
              ref={secureInputRef}
              type="file"
              onChange={(e) => pickFile(e.target.files?.[0], "secure")}
            />
            <span>↑ <b>{secureFile ? secureFile.name : "Select a file"}</b> or drop it here</span>
          </label>

          <div className="secure-fields">
            <label>
              Expires (hours)
              <input
                type="number"
                min="1"
                max="720"
                step="1"
                value={secureExp}
                onChange={(e) => setSecureExp(e.target.value)}
              />
            </label>

            <label>
              Max downloads
              <input
                type="number"
                min="1"
                step="1"
                value={max}
                onChange={(e) => setMax(e.target.value)}
                placeholder="1"
              />
            </label>
          </div>

          <div className="access-toggle">
            <div>
              <b>Limit to authorized users</b>
              <small>Turn off to allow any logged-in user.</small>
            </div>
            <button
              type="button"
              className={`switch ${restrict ? "on" : ""}`}
              onClick={() => setRestrict((current) => !current)}
              aria-label="Toggle authorized-user restriction"
            >
              <span />
            </button>
          </div>

          {restrict && (
            <div className="email-area">
              <div className="email-label">Authorized email addresses</div>
              <div className="email-add">
                <input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addEmail();
                    }
                  }}
                  placeholder="user@example.com"
                  type="email"
                />
                <button type="button" className="btn primary" onClick={addEmail}>+</button>
              </div>

              <div className="email-list">
                {emails.map((item) => (
                  <span className="email-chip" key={item}>
                    {item}
                    <button type="button" onClick={() => removeEmail(item)} aria-label={`Remove ${item}`}>×</button>
                  </span>
                ))}
              </div>
            </div>
          )}

          <div className="chips">
            <span>✓ Multiple users</span>
            <span>✓ Download limit</span>
            <span>✓ Expiry control</span>
            <span>✓ Revoke access</span>
          </div>

          <button className="btn primary wide" onClick={runSecure} disabled={secureBusy}>
            {secureBusy ? "Creating..." : "Create Secure Share →"}
          </button>

          <small className="center-note">
            {isLoggedIn() ? "You are logged in." : "Login is required before creating a Secure Share."}
          </small>

          <Result result={secureRes} />
        </div>
      </section>

      <section className="features" id="features">
        <div className="feature-collage">
          <div className="feature-tile protected-tile">
            <span className="feature-icon feature-art protected-art" aria-hidden="true"><span>▣</span></span>
            <span><b>Protected access</b><small>Secure shares can be limited to selected email addresses.</small></span>
          </div>
          <div className="feature-tile expiry-tile">
            <span className="feature-icon feature-art expiry-art" aria-hidden="true"><span>◷</span></span>
            <span><b>Expiry control</b><small>Shares expire automatically.</small></span>
          </div>
          <div className="feature-tile revoke-tile">
            <span className="feature-icon feature-art revoke-art" aria-hidden="true"><span>↻</span></span>
            <span><b>Revoke anytime</b><small>Owner can revoke a secure share.</small></span>
          </div>
          <div className="feature-tile history-tile">
            <span className="feature-icon feature-art history-art" aria-hidden="true"><span>↗</span></span>
            <span><b>Download history</b><small>Track download activity from your account.</small></span>
          </div>
        </div>
      </section>

      {error && (
        <div className="toast">
          {error}
          <button onClick={() => setError("")}>×</button>
        </div>
      )}
    </main>
  );
}
