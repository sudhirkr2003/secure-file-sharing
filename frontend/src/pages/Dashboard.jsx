import React, { useEffect, useMemo, useState } from "react";
import {
  getShareHistory,
  getDownloadHistory,
  updateExpiry,
  revokeShare,
  getUser,
  downloadUrl
} from "../services/api";

function fmt(value) {
  return value ? new Date(value).toLocaleString() : "—";
}

async function copyText(value) {
  try {
    await navigator.clipboard.writeText(value);
    return true;
  } catch (_) {
    return false;
  }
}

function userName(item) {
  if (!item) return "Unknown user";
  if (typeof item === "string") return item;
  return (
    item.name ||
    item.fullName ||
    item.userName ||
    item.user?.name ||
    (item.id ? `User #${item.id}` : "Unknown user")
  );
}

function allowedUserLabel(item) {
  if (!item) return "Authorized user";
  if (typeof item === "string") return item;
  return (
    item.name ||
    item.fullName ||
    item.userName ||
    item.user?.name ||
    item.email ||
    item.user?.email ||
    (item.id ? `User #${item.id}` : "Authorized user")
  );
}

function allowedUsersOf(share) {
  if (Array.isArray(share.allowedUsers)) return share.allowedUsers;
  if (Array.isArray(share.allowedUserEmails)) return share.allowedUserEmails;
  if (Array.isArray(share.allowedUserIds)) return share.allowedUserIds;
  return [];
}

function downloadedUsersOf(share) {
  return Array.isArray(share.downloadedBy) ? share.downloadedBy : [];
}

function AccessIcon({ share }) {
  if (share.quickShare) return <span className="access-icon public" title="Public Quick Share">🌐</span>;

  const users = allowedUsersOf(share);

  if (!users.length) {
    return <span className="access-icon logged-user" title="Any logged-in user">👤</span>;
  }

  return <span className="access-icon restricted" title="Specific authorized users">👤🔒</span>;
}

function DetailModal({ title, subtitle, items, emptyText, onClose }) {
  return (
    <div className="modal" onMouseDown={onClose}>
      <div className="modal-box detail-modal" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-title-row">
          <div>
            <div className="kicker">ACCESS DETAILS</div>
            <h3>{title}</h3>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button className="modal-close" onClick={onClose} aria-label="Close">×</button>
        </div>

        {items.length ? (
          <ol className="user-list">
            {items.map((item, index) => (
              <li key={`${userName(item)}-${index}`}>
                <span className="user-avatar">👤</span>
                <span>{title === "Authorized users" ? allowedUserLabel(item) : userName(item)}</span>
              </li>
            ))}
          </ol>
        ) : (
          <div className="empty-detail">{emptyText}</div>
        )}

        <div className="actions">
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const [shares, setShares] = useState([]);
  const [downloads, setDownloads] = useState([]);
  const [err, setErr] = useState("");
  const [copied, setCopied] = useState("");
  const [edit, setEdit] = useState(null);
  const [hours, setHours] = useState("");
  const [detail, setDetail] = useState(null);
  const user = getUser();

  async function load() {
    try {
      const [shareData, downloadData] = await Promise.all([
        getShareHistory(),
        getDownloadHistory()
      ]);
      setShares(Array.isArray(shareData) ? shareData : []);
      setDownloads(Array.isArray(downloadData) ? downloadData : []);
      setErr("");
    } catch (error) {
      setErr(error.message || "Unable to load dashboard.");
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function revoke(token) {
    if (!window.confirm("Revoke this secure share?")) return;

    try {
      await revokeShare(token);
      await load();
    } catch (error) {
      setErr(error.message || "Unable to revoke share.");
    }
  }

  function remainingExtensionHours(share) {
    // Maximum lifetime is 720 hours from creation.
    // This is a frontend guard; the backend must enforce the same rule.
    const created = share.createdAt ? new Date(share.createdAt).getTime() : NaN;
    if (Number.isNaN(created)) return 720;

    const maxEnd = created + 720 * 60 * 60 * 1000;
    const remaining = Math.floor((maxEnd - Date.now()) / (60 * 60 * 1000));
    return Math.max(0, Math.min(720, remaining));
  }

  function openExpiry(share) {
    setEdit(share);
    setHours("");
  }

  async function expiry() {
    if (!edit) return;

    const value = Number(hours);
    const remaining = remainingExtensionHours(edit);

    if (!/^\d+$/.test(String(hours)) || value < 1) {
      setErr("Enter at least 1 hour.");
      return;
    }

    if (value > remaining) {
      setErr(
        remaining > 0
          ? `You can extend this share by at most ${remaining} more hour${remaining === 1 ? "" : "s"}.`
          : "This share has reached the maximum 30-day lifetime."
      );
      return;
    }

    try {
      await updateExpiry(edit.shareToken, value);
      setEdit(null);
      setHours("");
      await load();
    } catch (error) {
      setErr(error.message || "Unable to update expiry.");
    }
  }

  async function copyShareLink(token) {
    if (!token) return;
    const ok = await copyText(downloadUrl(token));

    if (ok) {
      setCopied(token);
      setTimeout(() => setCopied(""), 1400);
    } else {
      setErr("Could not copy the share link.");
    }
  }

  const activeCount = useMemo(
    () =>
      shares.filter(
        (share) =>
          !share.revoked &&
          new Date(share.expiresAt) > new Date()
      ).length,
    [shares]
  );

  return (
    <main className="page dashboard-page">
      <div className="dashboard-head">
        <div>
          <div className="kicker">ACCOUNT</div>
          <h1>My Dashboard</h1>
          <p>{user.email}</p>
        </div>
      </div>

      {err && (
        <div className="notice danger dashboard-error">
          {err}
          <button className="inline-action" onClick={() => setErr("")}>Dismiss</button>
        </div>
      )}

      <div className="stats">
        <div><b>{shares.length}</b><span>Total shares</span></div>
        <div><b>{activeCount}</b><span>Active shares</span></div>
        <div><b>{downloads.length}</b><span>My downloads</span></div>
      </div>

      <div className="panel">
        <div className="panel-head">
          <div>
            <h2>My Shares</h2>
            <p className="panel-subtitle">Manage active shares and inspect access without exposing unnecessary details.</p>
          </div>
          <a className="btn primary" href="/#secure">Create Secure Share</a>
        </div>

        <div className="table-wrap">
          <table className="share-table">
            <thead>
              <tr>
                <th>File</th>
                <th>Type</th>
                <th>Expiry</th>
                <th>Downloads</th>
                <th>Access</th>
                <th>Status</th>
                <th>Share link</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {shares.map((share) => {
                const expired = new Date(share.expiresAt) <= new Date();
                const status = share.revoked ? "Revoked" : expired ? "Expired" : "Active";
                const active = status === "Active";
                const allowed = allowedUsersOf(share);
                const downloaded = downloadedUsersOf(share);

                return (
                  <tr key={share.id || share.shareToken}>
                    <td className="file-cell">{share.file?.originalFileName || share.fileName || "—"}</td>
                    <td><span className={`type-badge ${share.quickShare ? "quick" : "secure"}`}>{share.quickShare ? "Quick" : "Secure"}</span></td>
                    <td>{fmt(share.expiresAt)}</td>

                    <td>
                      <div className="count-action">
                        <span>{share.downloadCount ?? 0}{share.maxDownloads ? ` / ${share.maxDownloads}` : ""}</span>
                        {!share.quickShare && (
                          <button
                            className="icon-btn"
                            title="View who downloaded this secure share"
                            aria-label="View downloaders"
                            onClick={() =>
                              setDetail({
                                title: "Downloaded by",
                                subtitle: share.file?.originalFileName || share.fileName || "Secure Share",
                                items: downloaded,
                                emptyText: "No one has downloaded this secure share yet."
                              })
                            }
                          >
                            👁
                          </button>
                        )}
                      </div>
                    </td>

                    <td>
                      <div className="access-cell">
                        <AccessIcon share={share} />
                        <button
                          className="icon-btn"
                          title={share.quickShare ? "Public share" : "View allowed users"}
                          aria-label="View access users"
                          disabled={share.quickShare}
                          onClick={() =>
                            setDetail({
                              title: "Authorized users",
                              subtitle: share.file?.originalFileName || share.fileName || "Secure Share",
                              items: allowed,
                              emptyText: "Any logged-in user can download this share."
                            })
                          }
                        >
                          👁
                        </button>
                      </div>
                    </td>

                    <td><span className={`status ${status.toLowerCase()}`}>{status}</span></td>

                    <td>
                      {active && share.shareToken ? (
                        <button className="mini-btn link-btn" onClick={() => copyShareLink(share.shareToken)}>
                          {copied === share.shareToken ? "Copied" : "Copy link"}
                        </button>
                      ) : <span className="muted-dash">—</span>}
                    </td>

                    <td>
                      {!share.quickShare && active && (
                        <>
                          <button className="mini-btn" onClick={() => openExpiry(share)}>Expiry</button>
                          <button className="mini-btn danger-btn" onClick={() => revoke(share.shareToken)}>Revoke</button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })}

              {!shares.length && (
                <tr><td colSpan="8" className="empty-cell">No shares yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {edit && (
        <div className="modal" onMouseDown={() => setEdit(null)}>
          <div className="modal-box" onMouseDown={(e) => e.stopPropagation()}>
            <div className="modal-title-row">
              <div>
                <div className="kicker">EXPIRY CONTROL</div>
                <h3>Extend expiry</h3>
              </div>
              <button className="modal-close" onClick={() => setEdit(null)}>×</button>
            </div>

            <p>
              Enter how many <b>additional hours from now</b> you want to add.
              The share cannot go beyond 30 days from its creation time.
            </p>

            <label className="modal-label">
              Additional hours
              <input
                className="modal-number"
                type="number"
                min="1"
                max={remainingExtensionHours(edit)}
                step="1"
                value={hours}
                onChange={(event) => setHours(event.target.value)}
                placeholder={`1–${remainingExtensionHours(edit)}`}
              />
            </label>

            <small className="modal-hint">
              Maximum additional time currently available: {remainingExtensionHours(edit)} hour{remainingExtensionHours(edit) === 1 ? "" : "s"}.
            </small>

            <div className="actions">
              <button className="btn primary" onClick={expiry}>Update</button>
              <button className="btn" onClick={() => setEdit(null)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {detail && (
        <DetailModal
          title={detail.title}
          subtitle={detail.subtitle}
          items={detail.items || []}
          emptyText={detail.emptyText}
          onClose={() => setDetail(null)}
        />
      )}

      <div className="panel download-history-panel">
        <div className="panel-head">
          <div>
            <h2>Download History</h2>
            <p className="panel-subtitle">Files you have downloaded from SecureShare.</p>
          </div>
        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>File</th><th>Type</th><th>Downloaded at</th><th>Share link</th></tr>
            </thead>
            <tbody>
              {downloads.map((download, index) => {
                const token = download.shareToken || download.token || download.fileShareToken;
                return (
                  <tr key={download.id || index}>
                    <td>{download.fileName || download.originalFileName || "—"}</td>
                    <td><span className={`type-badge ${download.quickShare ? "quick" : "secure"}`}>{download.quickShare ? "Quick" : "Secure"}</span></td>
                    <td>{fmt(download.downloadedAt)}</td>
                    <td>
                      {token ? (
                        <button className="mini-btn link-btn" onClick={() => copyShareLink(token)}>Copy link</button>
                      ) : <span className="muted-dash">—</span>}
                    </td>
                  </tr>
                );
              })}
              {!downloads.length && (
                <tr><td colSpan="4" className="empty-cell">No downloads yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </main>
  );
}
