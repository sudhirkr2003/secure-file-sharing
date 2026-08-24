import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { login } from "../services/api";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setErr("");

    try {
      await login(email.trim(), password);

      // Do NOT reopen/paste the old share URL.
      // Home will pick the token from sessionStorage and continue automatically.
      const after = sessionStorage.getItem("ss_after_login");
      if (after) {
        sessionStorage.removeItem("ss_after_login");
      }

      navigate(after || "/", { replace: true });
    } catch (error) {
      setErr(error.message || "Login failed.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <div className="kicker">ACCOUNT ACCESS</div>
        <h1>Welcome back</h1>
        <p>Login to create secure shares and access protected files.</p>

        {err && <div className="notice danger">{err}</div>}

        <form onSubmit={submit}>
          <label>
            Email
            <input
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              type="email"
              autoComplete="email"
            />
          </label>

          <label>
            Password
            <input
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              type="password"
              autoComplete="current-password"
            />
          </label>

          <button className="btn primary wide" disabled={busy}>
            {busy ? "Logging in..." : "Login"}
          </button>
        </form>

        <Link className="forgot" to="/forgot-password">
          Forgot password?
        </Link>

        <div className="auth-foot">
          Don't have an account? <Link to="/register">Register</Link>
        </div>
      </div>
    </main>
  );
}
