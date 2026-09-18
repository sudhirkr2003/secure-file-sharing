import React, { useEffect, useState } from "react";
import { api } from "../services/api";

export default function Docs() {
  const [documentation, setDocumentation] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    api("/api/doc")
      .then((data) => {
        if (active) setDocumentation(data);
      })
      .catch(() => {
        if (active) setError("Documentation is temporarily unavailable.");
      });

    return () => {
      active = false;
    };
  }, []);

  if (error) {
    return (
      <main className="docs-page page">
        <div className="docs-error">
          <div className="kicker">API DOCUMENTATION</div>
          <h1>Documentation unavailable</h1>
          <p>{error}</p>
        </div>
      </main>
    );
  }

  if (!documentation) {
    return (
      <main className="route-loading">
        <div className="loader-card"><div className="spinner" /><b>Loading API documentation...</b></div>
      </main>
    );
  }

  const groups = documentation.endpoints.reduce((result, endpoint) => {
    const category = endpoint.path.startsWith("/api/auth/")
      ? "Authentication"
      : endpoint.path.startsWith("/api/files/")
        ? "File Sharing"
        : "System & Diagnostics";

    if (!result[category]) result[category] = [];
    result[category].push(endpoint);
    return result;
  }, {});

  const apiBase = import.meta.env.VITE_API_URL || import.meta.env.VITE_API_BASE || "https://secure-file-sharing-backend-0bfq.onrender.com";

  return (
    <main className="docs-page page">
      <header className="docs-hero">
        <div>
          <div className="docs-brand">SECURE<span>SHARE</span> DOCUMENTATION</div>
          <h1>Complete API Documentation</h1>
          <p>{documentation.description} Explore authentication, file sharing, health checks, and secure access rules in one place.</p>
        </div>
        <div className="docs-status">
          <span className="docs-status-dot" />
          <b>API Status: Online</b>
          <small>Documentation endpoint responding</small>
        </div>
      </header>

      <div className="docs-meta-grid">
        <div><b>Version</b><span>1.0.0</span></div>
        <div><b>Backend URL</b><code>{apiBase}</code></div>
        <div><b>Frontend URL</b><code>https://secure-file-sharing-frontend-uj9g.onrender.com</code></div>
        <div><b>Total Endpoints</b><span>{documentation.endpointCount}</span></div>
        <div><b>Authentication</b><span>JWT Bearer Token</span></div>
      </div>

      <section className="docs-section">
        <div className="docs-section-title"><span>✦</span><h2>Key Features</h2></div>
        <div className="docs-feature-grid">
          <div><b>🔐 JWT Authentication</b><span>Secure private operations with Bearer tokens.</span></div>
          <div><b>📦 Controlled File Sharing</b><span>Quick and secure shares with expiry controls.</span></div>
          <div><b>✉ OTP Protection</b><span>Registration and password recovery verification.</span></div>
          <div><b>📱 Responsive Frontend</b><span>Readable documentation on desktop and mobile.</span></div>
        </div>
      </section>

      <div className="docs-auth-note">
        <b>🔑 Authentication</b>
        <span>Private endpoints require <code>Authorization: Bearer &lt;JWT_TOKEN&gt;</code>.</span>
      </div>

      {Object.entries(groups).map(([category, endpoints]) => (
        <section className="docs-section" key={category}>
          <div className="docs-section-title"><span>{category === "Authentication" ? "🔑" : category === "File Sharing" ? "📁" : "⚙"}</span><h2>{category}</h2></div>
          <div className="docs-list" aria-label={`${category} endpoints`}>
            {endpoints.map((endpoint) => (
              <article className="docs-endpoint" key={`${endpoint.method}-${endpoint.path}`}>
                <div className="docs-endpoint-top">
                  <span className={`docs-method ${endpoint.method.toLowerCase()}`}>{endpoint.method}</span>
                  <code className="docs-path">{endpoint.path}</code>
                  <span className={`docs-access ${endpoint.access.toLowerCase().replace(/[^a-z]+/g, "-")}`}>
                    {endpoint.access}
                  </span>
                </div>
                <h3>{endpoint.summary}</h3>
                <div className="docs-details">
                  <div><b>Request</b><span>{endpoint.request}</span></div>
                  <div><b>Behavior</b><span>{endpoint.notes}</span></div>
                </div>
              </article>
            ))}
          </div>
        </section>
      ))}

      <footer className="docs-footer"><b>Built with</b><span>React</span><span>Spring Boot</span><span>Spring Security</span><span>JWT</span><span>MySQL / PostgreSQL</span></footer>
    </main>
  );
}
