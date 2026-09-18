import React, { useEffect, useState } from "react";
import { checkHealth } from "../services/api";

export default function HealthPage() {
  const [state, setState] = useState({ status: "checking", message: "Checking backend..." });

  useEffect(() => {
    let active = true;

    checkHealth()
      .then((message) => {
        if (active) setState({ status: "online", message });
      })
      .catch((error) => {
        if (active) {
          setState({
            status: "offline",
            message: error.message || "Backend is unavailable."
          });
        }
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="health-page page">
      <section className={`health-card ${state.status}`}>
        <div className="kicker">BACKEND HEALTH</div>
        <div className="health-icon" aria-hidden="true">{state.status === "online" ? "✓" : state.status === "offline" ? "!" : "..."}</div>
        <h1>{state.status === "online" ? "Backend is running" : state.status === "offline" ? "Backend unavailable" : "Checking backend"}</h1>
        <p>{state.message}</p>
        <code>GET /api/health</code>
      </section>
    </main>
  );
}
