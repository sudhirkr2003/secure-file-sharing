import React from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { SecureShareDraftProvider } from "./context/SecureShareDraftContext";
import "./styles/style.css";
createRoot(document.getElementById("root")).render(
  <BrowserRouter>
    <SecureShareDraftProvider>
      <App />
    </SecureShareDraftProvider>
  </BrowserRouter>
);
