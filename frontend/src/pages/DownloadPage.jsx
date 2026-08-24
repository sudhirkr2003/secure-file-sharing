import React, { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";

// Kept as a tiny compatibility route. New share links use /download/:token,
// which is handled by DownloadRedirect in App.jsx and always lands on Home.
export default function DownloadPage() {
  const { token } = useParams();
  const navigate = useNavigate();

  useEffect(() => {
    if (token) {
      sessionStorage.setItem("ss_pending_download_token", token);
    }
    navigate("/", { replace: true });
  }, [token, navigate]);

  return (
    <main className="route-loading">
      <div className="loader-card">
        <div className="spinner" />
        <b>Opening SecureShare...</b>
      </div>
    </main>
  );
}
