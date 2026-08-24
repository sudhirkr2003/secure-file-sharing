import React, { useEffect, useRef, useState } from "react";
import {
  Routes,
  Route,
  Navigate,
  useNavigate,
  useLocation,
  Link,
  useParams
} from "react-router-dom";
import { getUser, isLoggedIn, logout as clearAuth } from "./services/api";
import Home from "./pages/Home";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ForgotPassword from "./pages/ForgotPassword";
import Dashboard from "./pages/Dashboard";
import DownloadPage from "./pages/DownloadPage";

function DownloadRedirect() {
  const { token } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    if (token) sessionStorage.setItem("ss_pending_download_token", token);
    navigate("/", { replace: true });
  }, [token, navigate]);

  return (
    <main className="route-loading">
      <div className="loader-card"><div className="spinner" /><b>Opening share link...</b></div>
    </main>
  );
}

function PublicOnly({ children }) {
  return isLoggedIn() ? <Navigate to="/" replace /> : children;
}

function Protected({ children }) {
  return isLoggedIn() ? children : <Navigate to="/login" replace />;
}

function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const profileRef = useRef(null);
  const menuRef = useRef(null);
  const menuButtonRef = useRef(null);

  const [logged, setLogged] = useState(isLoggedIn());
  const [user, setUser] = useState(getUser());
  const [profileOpen, setProfileOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [theme, setTheme] = useState(() => localStorage.getItem("ss_theme") || "light");

  useEffect(() => {
    setLogged(isLoggedIn());
    setUser(getUser());
    setProfileOpen(false);
    setMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    function refreshAuth() {
      const authenticated = isLoggedIn();
      setLogged(authenticated);
      setUser(getUser());
      if (!authenticated && location.pathname === "/dashboard") {
        navigate("/login", { replace: true });
      }
    }

    window.addEventListener("ss-auth-changed", refreshAuth);
    return () => window.removeEventListener("ss-auth-changed", refreshAuth);
  }, [location.pathname, navigate]);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("ss_theme", theme);
  }, [theme]);

  useEffect(() => {
    function closeOnOutsideClick(event) {
      if (profileRef.current && !profileRef.current.contains(event.target)) {
        setProfileOpen(false);
      }
      if (
        mobileMenuOpen &&
        menuRef.current &&
        !menuRef.current.contains(event.target) &&
        menuButtonRef.current &&
        !menuButtonRef.current.contains(event.target)
      ) {
        setMobileMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", closeOnOutsideClick);
    return () => document.removeEventListener("mousedown", closeOnOutsideClick);
  }, [mobileMenuOpen]);

  function logout() {
    clearAuth();
    setLogged(false);
    setUser({ email: "", name: "" });
    setProfileOpen(false);
    navigate("/");
  }

  const displayName = user.name || user.email?.split("@")[0] || "User";
  const profileInitial = displayName.trim().charAt(0).toUpperCase() || "U";

  return (
    <div className="app">
      <header className="nav">
        <Link className="logo" to="/">Secure<span>Share</span></Link>

        <nav className="navlinks">
          <a href="/#quick">Quick Share</a>
          <a href="/#secure">Secure Share</a>
          <a href="/#features">Features</a>
          <a href="/#download">Download</a>
        </nav>

        <div className="nav-tools">
          <button
            className="menu-toggle"
            ref={menuButtonRef}
            onClick={() => setMobileMenuOpen((open) => !open)}
            aria-label="Open navigation menu"
            aria-expanded={mobileMenuOpen}
          >
            <span>☰</span>
            <small>Menu</small>
          </button>

          <button
            className="theme-toggle"
            onClick={() => setTheme((current) => current === "light" ? "dark" : "light")}
            title={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
            aria-label="Toggle dark mode"
          >
            <span>{theme === "light" ? "☾" : "☀"}</span>
            <small>{theme === "light" ? "Dark" : "Light"}</small>
          </button>

          <div className="account">
            {logged ? (
              <div className="account-wrap" ref={profileRef}>
                <button
                  className="account-btn"
                  onClick={() => setProfileOpen((open) => !open)}
                  aria-expanded={profileOpen}
                  aria-label={`Open profile for ${displayName}`}
                  title={displayName}
                >
                  <span className="profile-avatar" aria-hidden="true">{profileInitial}</span>
                </button>

                {profileOpen && (
                  <div className="account-menu">
                    <div className="account-user">
                      <b>{user.name || "SecureShare user"}</b>
                      <small>{user.email}</small>
                    </div>
                    <Link to="/dashboard">Dashboard</Link>
                    <a href="/#quick">Quick Share</a>
                    <a href="/#secure">Secure Share</a>
                    <a href="/#download">Download</a>
                    <button onClick={logout}>Logout</button>
                  </div>
                )}
              </div>
            ) : (
              <>
                <Link className="btn ghost" to="/login">Login</Link>
                <Link className="btn primary" to="/register">Register</Link>
              </>
            )}
          </div>
        </div>

        {mobileMenuOpen && (
          <div className="mobile-nav-menu" ref={menuRef}>
            <a href="/#quick" onClick={() => setMobileMenuOpen(false)}>Quick Share</a>
            <a href="/#secure" onClick={() => setMobileMenuOpen(false)}>Secure Share</a>
            <a href="/#features" onClick={() => setMobileMenuOpen(false)}>Features</a>
            <a href="/#download" onClick={() => setMobileMenuOpen(false)}>Download</a>
          </div>
        )}
      </header>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
        <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/dashboard" element={<Protected><Dashboard /></Protected>} />
        <Route path="/download/:token" element={<DownloadRedirect />} />
        <Route path="/download" element={<DownloadPage />} />
      </Routes>

      <footer className="footer">
        <div className="footer-inner">
          <div className="footer-brand">
            <b>Secure<span>Share</span></b>
            <p>Simple, controlled file sharing.</p>
          </div>
          <div className="footer-links">
            <a href="/#quick">⚡ Quick Share</a>
            <a href="/#secure">🔐 Secure Share</a>
            <a href="/#features">🛡 Protection</a>
            <a href="/#download">↗ Download</a>
          </div>
          <div className="footer-note">© {new Date().getFullYear()} SecureShare</div>
        </div>
      </footer>
    </div>
  );
}

export default Layout;
