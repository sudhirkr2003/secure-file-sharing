# SecureShare

SecureShare is a secure file-sharing platform built with React and Spring Boot. It lets users upload files and create controlled sharing links with authentication, OTP verification, expiry controls, download limits, access restrictions, and download history.

For the complete API reference, including all endpoints, public/private access rules, request fields, response shapes, validation rules, and curl examples, see [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

## Features

- Quick Share links for simple, unauthenticated downloads
- Secure Share links protected by JWT authentication
- OTP verification for registration and password recovery
- Six-box OTP input with paste support
- Two-minute resend protection enforced by the backend
- Password reset with password confirmation validation
- File expiry and share revocation
- Per-user download limits for Secure Shares
- Optional allowed-email access control
- Download history and downloader information
- Local filesystem storage for development
- Cloudflare R2 storage for production
- MySQL support locally and PostgreSQL support in production
- SMTP email delivery with branded HTML OTP emails
- Render deployment configuration and Docker support

## Architecture

```text
React frontend
      |
      | HTTPS REST API
      v
Spring Boot backend
   |        |        |
   v        v        v
Database   Storage  SMTP email
```

### Development

```text
React + Vite -> Spring Boot -> MySQL + local uploads
```

### Production

```text
Render Static Site -> Render Web Service -> Supabase PostgreSQL
                                      -> Cloudflare R2
                                      -> SMTP provider
```

## Technology Stack

- Frontend: React, Vite, React Router
- Backend: Java 17, Spring Boot, Spring Security, Spring Data JPA
- Authentication: JWT
- Local database: MySQL
- Production database: Supabase PostgreSQL
- Local storage: filesystem
- Production storage: Cloudflare R2 using the S3 API
- Email: SMTP with Jakarta Mail
- Deployment: Render and Docker

## Project Structure

```text
.
├── backend/
│   ├── config/
│   │   ├── application-local.properties.example
│   │   └── production.env.example
│   ├── src/main/java/com/securefilesharing/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/
│   │   ├── context/
│   │   ├── pages/
│   │   ├── services/
│   │   └── styles/
│   ├── .env.example
│   └── package.json
├── render.yaml
├── DEPLOYMENT.md
└── .gitignore
```

## Local Development

### Prerequisites

- Java 17 or newer
- MySQL 8 or compatible MySQL server
- Node.js and npm
- A Gmail App Password or another SMTP account if email delivery is enabled

### Configure the backend

From the repository root:

```powershell
Copy-Item backend/config/application-local.properties.example backend/config/application-local.properties
```

Open `backend/config/application-local.properties` and set your local values:

```properties
DB_URL=jdbc:mysql://localhost:3306/secure_file_sharing?createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=your-local-database-password
JWT_SECRET=your-long-random-local-secret
MAIL_ENABLED=false
STORAGE_TYPE=local
FRONTEND_URL=http://localhost:5173
```

Set `MAIL_ENABLED=true` and provide SMTP values when you want to test real emails.

### Start the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend runs at `http://localhost:8080`.

### Start the frontend

In a second terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend normally runs at `http://localhost:5173`. If that port is busy, Vite may use another port. The backend CORS configuration includes the common local Vite ports.

## Environment Variables

### Frontend

Set `VITE_API_URL` for production:

```text
VITE_API_URL=https://your-backend.onrender.com
```

The frontend falls back to `http://localhost:8080` during local development.

Never put these values in the frontend:

```text
DB_PASSWORD
JWT_SECRET
R2_ACCESS_KEY
R2_SECRET_KEY
MAIL_PASSWORD
```

### Backend production

Set these in the Render backend service environment, not in source files:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://YOUR_SUPABASE_HOST:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=YOUR_SUPABASE_PASSWORD
JWT_SECRET=YOUR_LONG_RANDOM_SECRET
STORAGE_TYPE=r2
R2_ENDPOINT=https://YOUR_ACCOUNT_ID.r2.cloudflarestorage.com
R2_REGION=auto
R2_ACCESS_KEY=YOUR_R2_ACCESS_KEY
R2_SECRET_KEY=YOUR_R2_SECRET_KEY
R2_BUCKET=YOUR_R2_BUCKET
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=YOUR_SMTP_USERNAME
MAIL_PASSWORD=YOUR_SMTP_APP_PASSWORD
FRONTEND_URL=https://your-frontend.onrender.com
```

See [backend/config/production.env.example](backend/config/production.env.example) for the complete placeholder template.

## Sharing Rules

### Quick Share

- Does not require login to download
- Intended lifetime: 1 to 24 hours
- No download limit
- No allowed-user list
- Cannot be revoked or modified after creation

### Secure Share

- Requires login to create
- Intended lifetime: 1 to 720 hours
- Maximum lifetime is 30 days from creation
- Maximum downloads are required
- Allowed users are matched by trimmed, lowercase email
- Duplicate email addresses are rejected
- Can be revoked
- Download history is recorded
- If no allowed users are configured, any authenticated user may download

## OTP and Password Security

- OTPs are stored as hashes, not as plain text
- OTPs expire after 10 minutes
- OTP verification attempts are limited
- New OTP requests are blocked for two minutes for the same email and purpose
- The cooldown is enforced in the backend and cannot be bypassed with a new browser
- Password reset requires matching new-password and confirmation fields
- JWT and provider credentials are backend environment variables only

## Deployment

The repository includes a Render Blueprint in [render.yaml](render.yaml). The intended deployment is:

```text
Frontend -> Render Static Site
Backend  -> Render Web Service
Database -> Supabase PostgreSQL
Files    -> Cloudflare R2
Email    -> SMTP
```

Recommended deployment order:

1. Create the Supabase PostgreSQL database.
2. Create the Cloudflare R2 bucket and restricted API token.
3. Configure the SMTP provider and App Password.
4. Deploy the Render backend with `SPRING_PROFILES_ACTIVE=prod`.
5. Deploy the Render frontend with `VITE_API_URL` set to the backend URL.
6. Set the backend `FRONTEND_URL` to the final frontend URL.
7. Redeploy the backend to apply CORS configuration.
8. Test authentication, OTP, upload, sharing, download, expiry, revocation, and password reset.

See [DEPLOYMENT.md](DEPLOYMENT.md) for the full deployment checklist.

## Security and Git Rules

Never commit or upload real credentials. These files are intentionally ignored:

```text
backend/config/application-local.properties
frontend/.env
```

Do not commit `backend/uploads/`, build output, dependency folders, or secrets. If credentials have ever been exposed, rotate them before deployment:

- Database password
- JWT secret
- SMTP App Password
- Cloudflare R2 access keys

## Validation

Backend tests:

```powershell
cd backend
.\mvnw.cmd clean test
```

Frontend production build:

```powershell
cd frontend
npm ci
npm run build
```

## License

No license is currently included. This project should be treated as proprietary unless the owner adds a license that explicitly permits reuse.
