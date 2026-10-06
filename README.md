# Real-Time Attendance Auth Service: API Guide

Backend API for user registration, email verification, login, authenticated facial onboarding, course management, geo-fenced session attendance, and PDF report generation.

**Base URL:** `https://real-time-attendance-auth-service.onrender.com`

All request and response bodies are JSON. Send `Content-Type: application/json` and include the Authorization header on protected requests.

> **Note:** The service runs on a free Render plan. After a period of inactivity, the first request can take 30 to 60 seconds while it wakes up. Show a loading state and avoid short timeouts.

---

## Typical User Flow

1. `POST /api/auth/register` creates the account and sends a verification token to the user's email.
2. `POST /api/auth/resend-verification` sends a new code if needed.
3. `POST /api/auth/verify-email` confirms the email using the verification token.
4. `POST /api/auth/login` returns a JWT token.
5. **For Students:** After logging in, the frontend can query `GET /api/auth/me` to check if `faceEnrolled` is true. If false, redirect the user to the facial capture page (`/onboard-face`) to submit their embedding via `POST /api/auth/onboard-face`.
6. **For Attendance:** Students discover courses via `GET /api/courses/mine`, obtain active session details, and check in using GPS coordinates, session code, and facial verification.
7. **For Lecturers:** Create courses, upload/confirm rosters, start geo-tagged attendance sessions, and download PDF reports.

---

## Authentication

Login returns a JWT in the `token` field. Store it and send it on all protected requests:

Authorization: Bearer


Tokens are valid for 24 hours.

---

## Auth Endpoints (`/api/auth`)

### Register
`POST /api/auth/register`

```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "role": "STUDENT"
}
Success (200):

JSON
{
  "message": "user registered successfully",
  "role": "STUDENT",
  "redirect": "/verify-email"
}
Login
POST /api/auth/login

JSON
{
  "emailOrUsername": "string",
  "password": "string"
}
Success (200):

JSON
{
  "message": "Login Successful!",
  "token": "<jwt>",
  "role": "STUDENT",
  "redirect": "/dashboard"
}
Verify Email
POST /api/auth/verify-email

JSON
{
  "email": "string",
  "token": "string"
}
Resend Verification Code
POST /api/auth/resend-verification?email=<email>

Onboard Face (Students only)
POST /api/auth/onboard-face
(Requires Bearer Token)

JSON
{
  "facialEmbedding": "string"
}
Get Current User Profile
GET /api/auth/me
(Requires Bearer Token)

Success (200): Returns user details including faceEnrolled boolean to check if the student needs to capture their face.

Course Endpoints (/api/courses)
Create Course (Lecturers only)
POST /api/courses
(Requires Bearer Token)

JSON
{
  "courseCode": "CSC301",
  "title": "Advanced Data Structures",
  "semester": "Rain"
}
Get My Enrolled Courses (Students)
GET /api/courses/mine
(Requires Bearer Token)

Upload Roster CSV
POST /api/courses/{courseCode}/roster-upload

Form-data parameter: file (MultipartFile)

Confirm Roster
POST /api/courses/{courseCode}/confirm-roster

Get Student Courses by ID
GET /api/courses/student/{studentId}

Request Course Access (Students)
POST /api/courses/{courseCode}/request-access?studentId=<id>

Handle Access Request (Lecturers)
POST /api/courses/requests/{requestId}/action?approve=<true/false>

Attendance & Geofencing Endpoints (/api/attendance)
Start Attendance Session (Lecturers)
POST /api/attendance/sessions
(Requires Bearer Token)

JSON
{
  "courseCode": "CSC301",
  "durationMinutes": 15
}
Close Attendance Session
POST /api/attendance/sessions/{sessionId}/close

Check In to an Active Session (Students)
POST /api/attendance/sessions/{sessionId}/check-ins
(Requires Bearer Token)

JSON
{
  "code": "A3F9X1",
  "latitude": 7.5182,
  "longitude": 4.5221,
  "facialEmbedding": "string"
}
Success (200):

JSON
{
  "sessionId": 14,
  "status": "PRESENT",
  "checkedInAt": "2026-10-06T12:30:00"
}
Get Session Records
GET /api/attendance/sessions/{sessionId}/records

Reports Endpoints (/api/reports)
Download Session Attendance PDF Report
GET /api/reports/sessions/{sessionId}/pdf

Returns a downloadable PDF file attachment (application/pdf).

Facial Embedding Format
facialEmbedding is sent as a JSON-stringified array of numbers (e.g., "[0.12, -0.43, 0.88, ...]"). Ensure the exact same embedding library and format are used during face onboarding and attendance verification.