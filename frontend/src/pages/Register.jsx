import React, { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import {
  requestRegistrationOtp,
  verifyRegistrationOtp,
  resendRegistrationOtp,
  rememberProfile,
} from "../services/api";
import OtpBoxes from "../components/OtpBoxes";

export default function Register() {
  const nav = useNavigate();

  const [step, setStep] = useState(1);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [otp, setOtp] = useState("");
  const [err, setErr] = useState("");
  const [busy, setBusy] = useState(false);
  const [resendIn, setResendIn] = useState(0);

  useEffect(() => {
    if (resendIn <= 0) return;

    const timer = setInterval(() => {
      setResendIn((value) => Math.max(0, value - 1));
    }, 1000);

    return () => clearInterval(timer);
  }, [resendIn]);

  async function send(e) {
    e.preventDefault();
    setBusy(true);
    setErr("");

    try {
      await requestRegistrationOtp({ name, email, password });
      rememberProfile(email, name);
      setStep(2);
      setResendIn(120);
    } catch (x) {
      setErr(x.message);
    } finally {
      setBusy(false);
    }
  }

  async function verify(e) {
    e.preventDefault();
    setBusy(true);
    setErr("");

    try {
      await verifyRegistrationOtp(email, otp);
      nav("/login");
    } catch (x) {
      setErr(x.message);
    } finally {
      setBusy(false);
    }
  }

  async function resend() {
    setBusy(true);
    setErr("");

    try {
      await resendRegistrationOtp({ name, email, password });
      setResendIn(120);
    } catch (x) {
      setErr(x.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-card">
        <div className="kicker">CREATE ACCOUNT</div>

        <h1>Register</h1>

        <p>Create your account to use Secure Share.</p>

        {err && <div className="notice danger">{err}</div>}

        {step === 1 ? (
          <form onSubmit={send}>
            <label>
              Name
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                autoComplete="name"
              />
            </label>

            <label>
              Email
              <input
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                type="email"
                autoComplete="username"
              />
            </label>

            <label>
              Password
              <input
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                type="password"
                autoComplete="new-password"
              />
            </label>

            <button className="btn primary wide" disabled={busy}>
              {busy ? "Sending..." : "Send OTP"}
            </button>
          </form>
        ) : (
          <form onSubmit={verify}>
            <div className="notice">
              OTP sent to <b>{email}</b>.
            </div>

            <label>
              OTP
              <OtpBoxes value={otp} onChange={setOtp} disabled={busy} />
            </label>

            <button className="btn primary wide" disabled={busy}>
              {busy ? "Verifying..." : "Verify OTP"}
            </button>

            <button
              type="button"
              className="link-button"
              onClick={resend}
              disabled={busy || resendIn > 0}
            >
              {resendIn > 0
                ? `Resend OTP in ${Math.floor(resendIn / 60)}:${String(
                    resendIn % 60
                  ).padStart(2, "0")}`
                : "Resend OTP"}
            </button>
          </form>
        )}

        <div className="auth-foot">
          Already have an account? <Link to="/login">Login</Link>
        </div>
      </div>
    </main>
  );
}