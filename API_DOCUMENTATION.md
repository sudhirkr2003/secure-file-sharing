# SecureShare API Documentation

This document describes the current SecureShare backend API based on the controller, security, DTO, and service implementations in this repository.

## 1. Project Overview

SecureShare is a file-sharing application with:

- React + Vite frontend
- Spring Boot REST backend
- JWT authentication
- OTP-based registration and password reset
- Quick Share links for public downloads
- Secure Share links for authenticated and optionally restricted downloads
- File-share history and download history
- Local filesystem or production object storage
- MySQL locally and PostgreSQL in production

## 2. Base URL

### Backend

```text
https://secure-file-sharing-backend-0bfq.onrender.com
```

### Frontend documentation page

```text
https://secure-file-sharing-frontend-uj9g.onrender.com/docs
```

The frontend uses the `VITE_API_URL` or `VITE_API_BASE` environment variable. In the deployed frontend, this should point to the Render backend URL above.

## 3. Authentication

Authenticated requests use a JWT returned by the login endpoint.

```http
Authorization: Bearer <JWT_TOKEN>
```

The frontend stores the token in `localStorage` under `ss_token` and automatically adds the header through `frontend/src/services/api.js`.

### Access labels used below

| Label | Meaning |
|---|---|
| Public | No login is required by Spring Security. |
| Private | A valid JWT is required. |
| Public route, conditional access | The URL is public, but the service applies additional rules depending on the share type. |

All `OPTIONS /**` CORS preflight requests are also public.

## 4. Endpoint Summary

There are **18 application endpoints** currently defined.

| # | Method | Endpoint | Access | Purpose |
|---:|---|---|---|---|
| 1 | POST | `/api/auth/register` | Public | Request registration OTP |
| 2 | POST | `/api/auth/register/resend-otp` | Public | Resend registration OTP |
| 3 | POST | `/api/auth/register/verify-otp` | Public | Verify OTP and create account |
| 4 | POST | `/api/auth/login` | Public | Authenticate and receive JWT |
| 5 | POST | `/api/auth/forgot-password` | Public | Request password-reset OTP |
| 6 | POST | `/api/auth/forgot-password/resend-otp` | Public | Resend password-reset OTP |
| 7 | POST | `/api/auth/forgot-password/verify-otp` | Public | Verify password-reset OTP |
| 8 | POST | `/api/auth/forgot-password/reset` | Public | Set a new password |
| 9 | GET | `/api/health` | Public | Check whether the backend is running |
| 10 | POST | `/api/files/quick-share` | Public | Upload a file and create a public quick-share link |
| 11 | GET | `/api/files/download/{shareToken}` | Public route, conditional access | Download a quick share or authorized secure share |
| 12 | POST | `/api/files/upload` | Private | Upload a file without creating a share |
| 13 | POST | `/api/files/share` | Private | Create an authenticated secure share |
| 14 | POST | `/api/files/share/{shareToken}/expiry` | Private | Extend an owned secure share |
| 15 | POST | `/api/files/share/{shareToken}/revoke` | Private | Revoke an owned secure share |
| 16 | GET | `/api/files/shares/history` | Private | List shares created by the logged-in user |
| 17 | GET | `/api/files/download-history` | Private | List downloads made by the logged-in user |
| 18 | GET | `/api/test/secure` | Private | Test JWT authentication |

## 5. Common Response and Error Formats

### Message response

Several successful authentication operations return:

```json
{
  "message": "OTP sent. Verify the OTP to complete registration."
}
```

### Error response

Validation failures and handled errors use this shape:

```json
{
  "status": 400,
  "message": "email: must be a valid email address",
  "timestamp": "2026-09-18T12:30:00"
}
```

Common status codes:

| Status | Meaning |
|---:|---|
| 200 | Request completed successfully |
| 201 | Resource/account created successfully |
| 400 | Validation failure or business-rule failure |
| 401 | Authentication required or unauthorized |
| 403 | Authenticated user is not allowed to access the resource |
| 404 | Endpoint or share/file was not found |
| 500 | Unexpected server-side error |

## 6. Authentication Endpoints

### 6.1 Request registration OTP

```http
POST /api/auth/register
Content-Type: application/json
```

Request:

```json
{
  "name": "Alex User",
  "email": "alex@example.com",
  "password": "Password@123"
}
```

Rules:

- `name` is required.
- `email` is required and must be valid.
- `password` must be at least 8 characters.
- The password must contain at least one letter, one number, and one special character.

Success: `200 OK`

```json
{
  "message": "OTP sent. Verify the OTP to complete registration."
}
```

### 6.2 Resend registration OTP

```http
POST /api/auth/register/resend-otp
Content-Type: application/json
```

Request: same fields as registration.

```json
{
  "name": "Alex User",
  "email": "alex@example.com",
  "password": "Password@123"
}
```

Success: `200 OK`

```json
{
  "message": "Registration OTP sent."
}
```

### 6.3 Verify registration OTP

```http
POST /api/auth/register/verify-otp
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com",
  "otp": "123456"
}
```

Rules:

- `email` is required and must be valid.
- `otp` must contain exactly 6 digits.

Success: `201 Created`

```json
{
  "id": 1,
  "name": "Alex User",
  "email": "alex@example.com"
}
```

### 6.4 Login

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com",
  "password": "Password@123"
}
```

Success: `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "name": "Alex User"
}
```

Use the returned `token` for private endpoints.

### 6.5 Request password-reset OTP

```http
POST /api/auth/forgot-password
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com"
}
```

Success: `200 OK`

```json
{
  "message": "If the email is registered, a password reset OTP has been sent."
}
```

### 6.6 Resend password-reset OTP

```http
POST /api/auth/forgot-password/resend-otp
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com"
}
```

Success: `200 OK`

```json
{
  "message": "If the email is registered, a new password reset OTP has been sent."
}
```

### 6.7 Verify password-reset OTP

```http
POST /api/auth/forgot-password/verify-otp
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com",
  "otp": "123456"
}
```

Success: `200 OK`

```json
{
  "message": "OTP verified. You can now reset your password."
}
```

### 6.8 Reset password

```http
POST /api/auth/forgot-password/reset
Content-Type: application/json
```

Request:

```json
{
  "email": "alex@example.com",
  "password": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

Rules:

- Password must be at least 8 characters.
- Password must contain a letter, number, and special character.
- `password` and `confirmPassword` must match.

Success: `200 OK`

```json
{
  "message": "Password reset successfully."
}
```

## 7. Health and Authentication Test Endpoints

### 7.1 Health check

```http
GET /api/health
```

Access: Public.

Success: `200 OK`

```text
Service is running!
```

The frontend displays this result as the navbar status indicator and checks it every 30 seconds.

### 7.2 Secure authentication test

```http
GET /api/test/secure
Authorization: Bearer <JWT_TOKEN>
```

Access: Private.

Success:

```text
JWT authentication is working
```

This endpoint is intended for testing JWT configuration and is not a business operation.

## 8. File and Share Endpoints

### 8.1 Create quick share

```http
POST /api/files/quick-share
Content-Type: multipart/form-data
```

Access: Public.

Form fields:

| Field | Required | Type | Rules |
|---|---|---|---|
| `file` | Yes | File | Must not be empty |
| `expiryHours` | No | Integer | Defaults to 24; allowed range is 1 to 24 |

Example with curl:

```bash
curl -X POST http://localhost:8080/api/files/quick-share \
  -F "file=@./example.pdf" \
  -F "expiryHours=12"
```

Success: `200 OK`, returns a `FileShareResponse`.

Important behavior:

- Anyone can create a quick share.
- Quick-share links can be downloaded without logging in.
- Quick shares cannot be revoked or extended through the management endpoints.
- Quick-share expiry defaults to 24 hours.

### 8.2 Upload file

```http
POST /api/files/upload
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data
```

Access: Private.

Form fields:

| Field | Required | Type |
|---|---|---|
| `file` | Yes | File |

Example:

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@./example.pdf"
```

Success: `200 OK`

```json
{
  "id": 10,
  "originalFileName": "example.pdf",
  "contentType": "application/pdf",
  "fileSize": 24576,
  "uploadedBy": 1
}
```

### 8.3 Create secure share

```http
POST /api/files/share
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data
```

Access: Private.

Form fields:

| Field | Required | Type | Rules |
|---|---|---|---|
| `file` | Yes | File | Must not be empty |
| `expiryHours` | No | Integer | Defaults to 24; allowed range is 1 to 720 |
| `maxDownloads` | Yes | Integer | Must be greater than 0 |
| `allowedUserEmails` | No | Repeated string field | Optional; each email must be valid and unique |

Example:

```bash
curl -X POST http://localhost:8080/api/files/share \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@./example.pdf" \
  -F "expiryHours=48" \
  -F "maxDownloads=5" \
  -F "allowedUserEmails=friend@example.com" \
  -F "allowedUserEmails=colleague@example.com"
```

Behavior:

- The logged-in user becomes the owner.
- If `allowedUserEmails` is empty, any logged-in user can download the share while it is active.
- If allowed emails are supplied, only the owner and those email addresses can download it.
- Each authorized user counts once toward `maxDownloads`.
- The initial expiry cannot exceed 720 hours.

### 8.4 Download a file

```http
GET /api/files/download/{shareToken}
```

Access: Public route with conditional service authorization.

Example:

```text
GET /api/files/download/550e8400-e29b-41d4-a716-446655440000
```

Response:

- Success: `200 OK`
- Body: File bytes
- `Content-Disposition`: attachment with the original filename
- `Content-Type`: Original file content type, or `application/octet-stream`

Access rules:

| Share type | Download requirement |
|---|---|
| Quick Share | No login required, provided the link has not expired |
| Secure Share without allowed users | Valid JWT required |
| Secure Share with allowed users | Valid JWT and the current user's email must be authorized |
| Secure Share owner | Owner can download their own active share |

Other rules:

- Expired links are rejected.
- Revoked secure shares are rejected.
- Secure-share downloads count once per user.
- A secure share is rejected after its download limit is reached, except that a user who already downloaded it can download again without increasing the count.

### 8.5 Update secure-share expiry

```http
POST /api/files/share/{shareToken}/expiry?expiryHours=72
Authorization: Bearer <JWT_TOKEN>
```

Access: Private; only the share owner may update it.

Rules:

- Quick Share cannot be modified.
- Revoked shares cannot be modified.
- Expired shares cannot be modified.
- `expiryHours` must be greater than 0.
- The new expiry cannot be later than 30 days from the share creation time.

Success: `200 OK`, returns the updated `FileShareResponse`.

### 8.6 Revoke secure share

```http
POST /api/files/share/{shareToken}/revoke
Authorization: Bearer <JWT_TOKEN>
```

Access: Private; only the share owner may revoke it.

Rules:

- Quick Share cannot be revoked.
- The share must not already be revoked.

Success: `200 OK`, returns the revoked `FileShareResponse` with `revoked: true`.

### 8.7 Get share history

```http
GET /api/files/shares/history
Authorization: Bearer <JWT_TOKEN>
```

Access: Private.

Returns all shares whose uploaded file belongs to the logged-in user.

Success: `200 OK`

```json
[
  {
    "id": 20,
    "file": {
      "id": 10,
      "originalFileName": "example.pdf",
      "contentType": "application/pdf",
      "fileSize": 24576,
      "uploadedBy": 1
    },
    "shareToken": "550e8400-e29b-41d4-a716-446655440000",
    "expiresAt": "2026-09-20T12:00:00",
    "quickShare": false,
    "maxDownloads": 5,
    "downloadCount": 1,
    "createdAt": "2026-09-18T12:00:00",
    "revoked": false,
    "allowedUsers": [
      {
        "name": null,
        "email": "friend@example.com"
      }
    ],
    "downloadedBy": [
      {
        "name": "Friend User"
      }
    ]
  }
]
```

### 8.8 Get download history

```http
GET /api/files/download-history
Authorization: Bearer <JWT_TOKEN>
```

Access: Private.

Success: `200 OK`

```json
[
  {
    "shareId": 20,
    "fileName": "example.pdf",
    "downloadedBy": "Friend User",
    "downloadedAt": "2026-09-18T13:15:00"
  }
]
```

## 9. FileShareResponse Reference

Endpoints that create or manage shares return this object:

| Field | Type | Description |
|---|---|---|
| `id` | Number | Share database ID |
| `file` | Object | Uploaded file metadata |
| `shareToken` | String | Token used in the download URL |
| `expiresAt` | Date-time | Share expiration time |
| `quickShare` | Boolean | `true` for quick share, `false` for secure share |
| `maxDownloads` | Number or null | Maximum unique-user downloads; null for quick share |
| `downloadCount` | Number | Number of counted unique-user downloads |
| `createdAt` | Date-time | Creation time |
| `revoked` | Boolean | Whether a secure share was revoked |
| `allowedUsers` | Array | Authorized email records |
| `downloadedBy` | Array | Users who have downloaded the share |

File metadata:

| Field | Type | Description |
|---|---|---|
| `id` | Number | File database ID |
| `originalFileName` | String | Original uploaded filename |
| `contentType` | String or null | MIME type |
| `fileSize` | Number | File size in bytes |
| `uploadedBy` | Number or null | Owner user ID |

## 10. Typical User Flows

### Registration

1. `POST /api/auth/register`
2. User receives OTP by email.
3. `POST /api/auth/register/verify-otp`
4. User logs in with `POST /api/auth/login`.

### Quick Share

1. `POST /api/files/quick-share`
2. Read `shareToken` from the response.
3. Build the URL `/api/files/download/{shareToken}`.
4. Share the URL with the recipient.

### Secure Share

1. User logs in.
2. Send JWT with `POST /api/files/share`.
3. Read `shareToken` from the response.
4. Share the download URL.
5. Owner can use the expiry and revoke endpoints.
6. Owner can view `/api/files/shares/history`.

### Password Reset

1. `POST /api/auth/forgot-password`
2. `POST /api/auth/forgot-password/verify-otp`
3. `POST /api/auth/forgot-password/reset`
4. Log in again with the new password.

## 11. CORS Configuration

The backend allows these local origins by default:

- `http://localhost:5500`
- `http://127.0.0.1:5500`
- `http://localhost:5173`
- `http://127.0.0.1:5173`
- `http://localhost:5174`
- `http://127.0.0.1:5174`
- The configured `frontend.url` value

Allowed methods are `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS`.

Allowed headers include `Authorization`, `Content-Type`, and `Accept`.

`Content-Disposition` is exposed so the frontend can read download filename information.

For production, configure:

```text
FRONTEND_URL=https://your-frontend.onrender.com
```

## 12. Running the Project Locally

### Backend

From the `backend` directory:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend normally starts on:

```text
http://localhost:8080
```

### Frontend

From the `frontend` directory:

```bash
npm install
npm run dev
```

The Vite development server normally starts on:

```text
http://localhost:5173
```

Set the frontend API URL when needed:

```text
VITE_API_URL=http://localhost:8080
```

## 13. Security Notes

- Never expose a JWT in a public URL.
- Store `JWT_SECRET` only in backend environment configuration.
- Use HTTPS in production.
- Keep database, SMTP, and object-storage credentials out of source control.
- Quick-share URLs are bearer links: anyone who has the link can download the file until it expires.
- Secure-share authorization is based on the logged-in user's email and JWT.
- The frontend status indicator only confirms that `/api/health` responds; it does not validate database, email, or storage availability.

## 14. Source of Truth

This document reflects the current implementation in:

- `backend/src/main/java/com/securefilesharing/controller/`
- `backend/src/main/java/com/securefilesharing/config/SecurityConfig.java`
- `backend/src/main/java/com/securefilesharing/config/CorsConfig.java`
- `backend/src/main/java/com/securefilesharing/dto/`
- `backend/src/main/java/com/securefilesharing/service/`

Update this document whenever a controller route, request DTO, authentication rule, or business rule changes.
