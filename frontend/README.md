# SecureShare React Frontend

## Run

```bash
npm install
npm run dev
```

Frontend: `http://localhost:5173`
Backend: `http://localhost:8080`

## Secure Share login/register draft behavior

The Secure Share creation draft is kept in React/browser memory at the app level.
This means the selected `File` object and the other Secure Share fields survive normal
SPA navigation from Home -> Login -> Register -> Login -> Home without uploading the
file before authentication.

After successful login, the user returns to `/#secure` and the existing draft is still
present. The user only needs to press **Create Secure Share** again.

Important: browser-memory persistence is intentionally session/tab scoped. A full page
refresh, closing the tab, or browser restart clears the selected File object. This avoids
an unnecessary pre-login upload and does not try to bypass browser file-input security.
