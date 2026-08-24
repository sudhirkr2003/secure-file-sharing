import React, { createContext, useContext, useMemo, useState } from "react";

const SecureShareDraftContext = createContext(null);

export function SecureShareDraftProvider({ children }) {
  // Browser-memory state. It survives Home -> Login -> Register -> Home
  // while the same React tab/app stays open, without uploading the file.
  const [secureFile, setSecureFile] = useState(null);
  const [secureExp, setSecureExp] = useState("24");
  const [max, setMax] = useState("1");
  const [restrict, setRestrict] = useState(true);
  const [emails, setEmails] = useState([]);

  const value = useMemo(
    () => ({
      secureFile,
      setSecureFile,
      secureExp,
      setSecureExp,
      max,
      setMax,
      restrict,
      setRestrict,
      emails,
      setEmails,
    }),
    [secureFile, secureExp, max, restrict, emails]
  );

  return (
    <SecureShareDraftContext.Provider value={value}>
      {children}
    </SecureShareDraftContext.Provider>
  );
}

export function useSecureShareDraft() {
  const context = useContext(SecureShareDraftContext);

  if (!context) {
    throw new Error(
      "useSecureShareDraft must be used inside SecureShareDraftProvider"
    );
  }

  return context;
}
