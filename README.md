# 🎓 OAU Smart Attendance System - Frontend Integration Guide

This document provides the complete API specifications, endpoints, and data payloads required to connect the frontend application to the Spring Boot backend.

---

## 🌐 Base URL & Configuration
* **Base API URL:** `http://localhost:2000/api` (Update this to your production URL when deployed).
* **Content-Type:** `application/json` (unless specified otherwise).

---

## 🔐 Authentication & Auth Endpoints

### 1. Register User
* **Endpoint:** `POST /api/auth/register`
* **Request Body:**
  ```json
  {
    "username": "student123",
    "email": "student@student.oauife.edu.ng",
    "password": "securePassword123",
    "matricNo": "CSC/2022/001",
    "role": "STUDENT"
  }
Success Response (200 OK):

JSON
{
"message": "user registered successfully",
"role": "STUDENT",
"redirectUrl": "/verify-email"
}
2. Verify Email (6-Digit OTP)
   Endpoint: POST /api/auth/verify-email

Request Body:

JSON
{
"email": "student@student.oauife.edu.ng",
"token": "482910"
}
Success Response (200 OK):

JSON
{
"message": "Email verified successfully!",
"role": "STUDENT",
"redirectUrl": "/facial-recognition"
}
3. Resend Verification Code
   Endpoint: POST /api/auth/resend-verification

Query Parameters: ?email=student@student.oauife.edu.ng

Success Response (200 OK): Plain text message ("A new verification has been sent to your email.")

4. Login
   Endpoint: POST /api/auth/login

Request Body:

JSON
{
"emailOrUsername": "student123",
"password": "securePassword123"
}
Success Response (200 OK):

JSON
{
"message": "Login Successful!",
"token": "eyJhbGciOiJIUzI1NiIs...",
"role": "STUDENT",
"redirectUrl": "/dashboard"
}
📸 Facial Biometrics & Attendance Endpoints
1. Facial Onboarding (Students Only)
   Endpoint: POST /api/auth/onboard-face

Request Body:

JSON
{
"username": "student123",
"facialEmbedding": "[0.123, -0.456, 0.789, ...]"
}
Success Response (200 OK):

JSON
{
"success": true,
"message": "Facial onboarding completed successfully! You can now access your dashboard."
}
2. Verify Face & Mark Attendance
   Endpoint: POST /api/attendance/verify-face

Request Body:

JSON
{
"username": "student123",
"facialEmbedding": "[0.124, -0.451, 0.782, ...]",
"courseCode": "CSC301"
}
Success Response (200 OK):

JSON
{
"success": true,
"message": "Attendance marked successfully for CSC/2022/001",
"timestamp": "2026-10-05T13:00:00"
}