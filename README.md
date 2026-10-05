# Real-Time Attendance Auth Service: API Guide

Backend API for user registration, email verification, login, facial onboarding and face-verified attendance.

**Base URL:** `https://real-time-attendance-auth-service.onrender.com`

All request and response bodies are JSON. Send `Content-Type: application/json` on every request that has a body.

> **Note:** The service runs on a free Render plan. After a period of inactivity the first request can take 30 to 60 seconds while it wakes up. Show a loading state and avoid short timeouts.

---

## Typical User Flow

1. `POST /api/auth/register` creates the account and sends a verification code to the user's email.
2. `POST /api/auth/resend-verification` sends a new code if needed.
3. `/api/auth/verify-email` confirms the email using the code.
   - Students are sent to `/onboard-face`.
   - Other roles are sent to `/dashboard`.
4. `POST /api/auth/onboard-face` (students only) saves the user's facial embedding.
5. `POST /api/auth/login` returns a JWT token.
6. `POST /api/attendance/verify-face` marks attendance for a course using a live facial embedding.

The `redirect` / next-route values in responses are frontend route suggestions. Use them for navigation.

---

## Authentication

Login returns a JWT in the `token` field. Store it and send it on protected requests:

```
Authorization: Bearer <token>
```

Tokens are valid for 24 hours (86,400,000 ms).

---

## Auth Endpoints

### Register

`POST /api/auth/register`

Request body (the exact fields come from the backend `RegisterRequest` DTO; confirm the full list with the backend developer):

```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "role": "STUDENT"
}
```

Success `200`:

```json
{
  "message": "user registered successfully",
  "role": "STUDENT",
  "redirect": "/verify-email"
}
```

Error `400`: a **plain text** message (not JSON), for example a duplicate email or username.

---

### Login

`POST /api/auth/login`

```json
{
  "emailOrUsername": "string",
  "password": "string"
}
```

Success `200`:

```json
{
  "message": "Login Successful!",
  "token": "<jwt>",
  "role": "STUDENT",
  "redirect": "/dashboard"
}
```

Error `401`: a **plain text** message (for example invalid credentials or unverified email).

---

### Verify Email

`/api/auth/verify-email`

```json
{
  "email": "string",
  "token": "string"
}
```

Success `200`:

```json
{
  "message": "Email verified successfully!",
  "role": "STUDENT",
  "redirect": "/onboard-face"
}
```

The next route is `/onboard-face` for students and `/dashboard` for everyone else.

Error `400`: a **plain text** message (for example an invalid or expired code).

> **Heads up:** this endpoint is currently declared as `GET` but expects a request body. Browsers and `fetch` do not allow a body on `GET` requests, so it will not work from a frontend as written. The backend should change it to `POST`. Until then, do not build against it.

---

### Resend Verification Code

`POST /api/auth/resend-verification?email=<email>`

The email is sent as a **query parameter**, not in the body.

Success `200`: plain text, `A new verification has been sent to your email.`

Error `400`: plain text message.

---

### Onboard Face (students)

`POST /api/auth/onboard-face`

```json
{
  "username": "string",
  "facialEmbedding": "string"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "Facial onboarding completed successfully! You can now access your dashboard."
}
```

Error `400`:

```json
{
  "success": false,
  "message": "reason for failure"
}
```

---

## Attendance Endpoint

### Verify Face and Mark Attendance

`POST /api/attendance/verify-face`

```json
{
  "username": "string",
  "facialEmbedding": "string",
  "courseCode": "string"
}
```

Success `200`:

```json
{
  "success": true,
  "message": "Attendance marked successfully for <matricNo>",
  "timestamp": "2026-10-05T12:30:00"
}
```

Error `400`:

```json
{
  "success": false,
  "message": "reason for failure"
}
```

---

## Facial Embedding Format

`facialEmbedding` is sent as a **string**. Agree with the backend developer on the exact format (for example a JSON-stringified array of numbers or a comma-separated list) and use the same format for onboarding and for attendance verification. A mismatch will cause verification to fail.

---

## Error Handling Summary

| Endpoint | Error status | Error body type |
| --- | --- | --- |
| `/api/auth/register` | 400 | plain text |
| `/api/auth/login` | 401 | plain text |
| `/api/auth/verify-email` | 400 | plain text |
| `/api/auth/resend-verification` | 400 | plain text |
| `/api/auth/onboard-face` | 400 | JSON `{ success, message }` |
| `/api/attendance/verify-face` | 400 | JSON `{ success, message }` |

Because error formats differ, read the response as text first when the status is not 2xx, then try to parse it as JSON.

---

## Example (fetch)

```javascript
const BASE_URL = "https://real-time-attendance-auth-service.onrender.com";

async function login(emailOrUsername, password) {
  const res = await fetch(`${BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ emailOrUsername, password }),
  });

  if (!res.ok) {
    throw new Error(await res.text());
  }
  return res.json(); // { message, token, role, redirect }
}
```

---