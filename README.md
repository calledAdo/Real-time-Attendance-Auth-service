# SmartAttend API

Spring Boot 4.1 / Java 17+ API for verified accounts, roster-assigned courses, attendance sessions and PDF reports. This branch adds the frontend integration contract to the engineer's auth service. The deployed Render service may still run an older revision.

## Run locally without PostgreSQL or email credentials

The `local` profile uses an in-memory H2 database and prints six-digit verification codes to the API process log. Data disappears when the process stops.

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

The API listens on `http://localhost:2000`. Run the frontend with `VITE_API_PROXY_TARGET=http://127.0.0.1:2000 npm run dev`. The test roster at `src/test/resources/roster.csv` contains an assigned student and an absent student. The local profile and its verification-code logging must never be enabled on a public deployment.

For a regular deployment, configure `POSTGRES_URL`, `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `JWT_SECRET`, `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REFRESH_TOKEN`, and `GMAIL_SENDER` as required by `application.properties` and `EmailService`. Review the existing PostgreSQL schema before allowing Hibernate `ddl-auto=update` to change it. No production database migration was run during this work.

## API contract

Protected routes require `Authorization: Bearer <token>`. The service gets the acting identity from the token subject. Student email and matric number must both match a confirmed roster entry. Session codes, lecturer coordinates, rosters and attendance records are shown only to course managers.

| Purpose | Route |
| --- | --- |
| Registration and email | `POST /api/auth/register`, `POST /api/auth/verify-email`, `POST /api/auth/resend-verification?email=...` |
| Login and profile | `POST /api/auth/login`, `GET /api/auth/me` |
| Student face enrollment | `POST /api/auth/onboard-face` with `{facialEmbedding:"[128 numbers]"}` |
| Course setup | `POST /api/courses`, `POST /api/courses/{courseCode}/roster-upload` (multipart `file`), `POST /api/courses/{courseCode}/confirm-roster` |
| Course lookup | `GET /api/courses/mine`, `GET /api/courses/{courseId}` |
| Sessions | `POST /api/attendance/sessions` with `{courseCode,latitude,longitude}`, `GET /api/attendance/sessions/active`, `GET /api/attendance/sessions/history`, `GET /api/attendance/sessions/{id}`, `POST /api/attendance/sessions/{id}/close` |
| Check-in | `POST /api/attendance/sessions/{id}/check-ins` with `{code,latitude,longitude,facialEmbedding}` |
| Reporting | `GET /api/attendance/sessions/{id}/records`, `GET /api/reports/sessions/{id}/pdf` |

Each session lasts five minutes. The server enforces a 100 m geofence around the lecturer's start location. Completed student history includes `myStatus` (`PRESENT` or `ABSENT`) and `myCheckedInAt`. The PDF uses the session's immutable roster snapshot and includes absent students. Browser-generated face descriptors do not prove liveness and GPS coordinates can be spoofed.

## Verification

`./mvnw test` runs focused attendance service tests. The local HTTP smoke test covered registration, verification, login, CSV roster, course assignment, synthetic face enrollment, start, geofence failure, successful and duplicate check-in, closure, history and PDF authorization. Real camera capture, deployed email delivery, production PostgreSQL migration and public CORS still require verification after deployment.
