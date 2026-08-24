# SecureShare Deployment

## Architecture

- Frontend: Render Static Site
- Backend: Render Web Service using `backend/Dockerfile`
- Database: Supabase PostgreSQL
- File storage: Cloudflare R2
- Email: SMTP, preferably a Gmail App Password

## Before deployment

1. Push the repository containing `backend/`, `frontend/`, and `render.yaml` to GitHub.
2. Create a Supabase PostgreSQL database and copy its connection details.
3. Create a Cloudflare R2 bucket and an API token with access only to that bucket.
4. Create an SMTP app password. Do not use the normal Gmail password.
5. Rotate any credentials that were previously placed in local files or chat.

## Render deployment order

### 1. Backend

Create or deploy the backend service from `render.yaml` and set:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL=jdbc:postgresql://HOST:5432/postgres?sslmode=require`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `STORAGE_TYPE=r2`
- `R2_ENDPOINT`, `R2_REGION`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `R2_BUCKET`
- `MAIL_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- `FRONTEND_URL` (set after the frontend URL exists, then redeploy backend)

The backend uses the Render-provided `PORT` through `backend/Dockerfile`.

### 2. Frontend

Create or deploy the frontend static site from `render.yaml` and set:

```text
VITE_API_URL=https://your-backend.onrender.com
```

The build command is `npm ci && npm run build` and the publish directory is `frontend/dist`.

### 3. Finish CORS

After Render provides the frontend URL, set the backend variable:

```text
FRONTEND_URL=https://your-frontend.onrender.com
```

Redeploy the backend and test login, OTP, upload, share, download, revoke, and password reset.

## Local development

Backend:

```powershell
cd backend
Copy-Item config/application-local.properties.example config/application-local.properties
# Fill local MySQL and SMTP values in application-local.properties
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```

Local defaults are MySQL, local `uploads/` storage, and backend `http://localhost:8080`.

## Security rules

- Never commit `backend/config/application-local.properties` or `.env` files.
- Use separate Supabase, R2, SMTP, and JWT values for production.
- Do not put database, R2, SMTP, or JWT secrets in the frontend.
- R2 credentials stay in the backend environment only.
