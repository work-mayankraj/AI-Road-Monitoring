# RoadGuard AI

Pothole detection + civic complaints (college project).

```
Citizen website (frontend/)  ──HTTP + JWT──▶  Spring Boot REST API (backend/)  ──JPA──▶  PostgreSQL
Administrator dashboard (frontend/admin.html) ─┘
```

| Part | Status |
|------|--------|
| `frontend/` – HTML + Tailwind + Leaflet | Done, now talks to the backend API |
| `backend/` – Spring Boot 3, Spring Security, JPA | **Phase 1 + 2 done** (REST APIs, PostgreSQL, JWT, roles) |
| `database/schema.sql` | Reference schema (the backend also creates the tables itself) |
| `ml-service/` – Python YOLO + OpenCV | Not built yet. Detection is a **sample/mock** engine behind the `DetectionEngine` interface |

## Run it

You need: Java 17+, Maven, PostgreSQL 14+ (or Docker), Node (only to serve the frontend).

**1. Database**
```bash
docker compose up -d          # PostgreSQL on localhost:5432, database "roadguard", user/password postgres/postgres
```
(or create a database named `roadguard` in your own PostgreSQL and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)

**2. Backend** – http://localhost:8080
```bash
cd backend
mvn spring-boot:run
```
Tables are created automatically. On first start it creates the demo accounts below.

**3. Frontend** – http://localhost:3000
```bash
cd frontend
npx serve .
```

If you serve the frontend on a different port, add that origin to `CORS_ORIGINS`
(already allowed: 3000, 5500, 8000 on localhost / 127.0.0.1). If the backend is not on
`localhost:8080`, put `<script>window.RG_API_BASE = 'http://host:port';</script>` before `app.js`.

### Demo accounts (created by the backend on first start)
- User: `user@demo.com` / `demo123`
- Administrator: `admin@demo.com` / `admin123`

Public registration always creates a normal user. The only way to get an administrator is the seeded
account (set `ADMIN_EMAIL` / `ADMIN_PASSWORD` before the first start), or changing `role` in the database.

### Configuration (environment variables)
| Variable | Default | Meaning |
|----------|---------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | local postgres | Database connection |
| `JWT_SECRET` | dev key in `application.properties` | Base64, ≥ 32 bytes. **Set your own** (`openssl rand -base64 32`) |
| `JWT_EXPIRATION_MINUTES` | 1440 | Token lifetime |
| `CORS_ORIGINS` | localhost:3000/5500/8000 | Comma-separated frontend origins |
| `UPLOAD_DIR` | `./uploads` | Where frame pictures are stored |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Admin User / admin@demo.com / admin123 | Seeded administrator |
| `SEED_DEMO_USER` | true | Create `user@demo.com` |
| `DDL_AUTO` | update | Use `validate` if you created tables from `database/schema.sql` |

## Phase 1 – Backend and database

**Entities** (`backend/src/main/java/com/roadguard/entity`)

| Entity | Table | Notes |
|--------|-------|-------|
| `User` | `users` | name, unique email, BCrypt password hash, role `USER`/`ADMIN` |
| `Upload` | `uploads`, `upload_frames` | one video or JPEG photo, its frames (picture URL + time) and photo location |
| `Detection` | `detections` | one pothole in one frame: confidence, box, location, status `PENDING/CONFIRMED/REJECTED` |
| `Complaint` | `complaints`, `complaint_detections` | many detections per complaint (a detection belongs to one complaint), severity, status `PENDING/IN_PROGRESS/RESOLVED` |

**REST API** (all JSON; everything except register/login and frame pictures needs `Authorization: Bearer <token>`)

| Method | Path | Who | What |
|--------|------|-----|------|
| POST | `/api/auth/register` | public | Create a user account, returns a token |
| POST | `/api/auth/login` | public | Returns a token (`role` field optional: `user`/`admin`) |
| GET | `/api/auth/me` | any | Current user |
| POST | `/api/uploads` | user | Create upload (+ frames as JPEG data URLs, GPS track) and run detection |
| GET | `/api/uploads`, `/api/uploads/{id}` | user | Own uploads |
| GET | `/api/detections`, `/api/detections/{id}` | user | Own detections |
| PATCH | `/api/detections/{id}/status` | user | `confirmed` / `rejected` / `pending` (locked once in a complaint) |
| POST | `/api/complaints` | user | `{detectionIds, description, severity}` – confirmed, own, not already reported |
| GET | `/api/complaints`, `/api/complaints/{id}` | user | Own complaints (`id` looks like `C-1001`) |
| PATCH | `/api/complaints/{id}` | user | Edit description while `pending` |
| GET | `/api/admin/complaints?status=` | admin | All complaints |
| PATCH | `/api/admin/complaints/{id}/status` | admin | `pending` / `in_progress` / `resolved` |
| GET | `/api/admin/detections`, `/uploads`, `/users` | admin | Everything, all users |
| GET | `/api/files/frames/{uuid}.jpg` | public link | Frame picture (random UUID name; `?download=1` to download) |

Errors are JSON: `{"status": 400, "message": "..."}`.

## Phase 2 – Authentication

- Registration and login with BCrypt-hashed passwords; login returns a signed **JWT** (HS256, contains email, name, role).
- `JwtAuthFilter` reads the `Authorization: Bearer` header on every request. The role is re-read from the database, so it can't be faked in the token.
- **Authorization:** `/api/admin/**` requires `ROLE_ADMIN` (403 otherwise); everything else requires a valid login (401 otherwise). Users only ever see or change their own uploads, detections and complaints – other people's ids answer 404.
- **Frontend:** `frontend/app.js` stores the token, loads data from the API into memory (`RG.ready`), and sends confirm/reject, uploads, complaints and admin status changes to the backend. The role checks in the browser only decide which page to show; the backend enforces the real permissions.

## Notes / next steps

- Detection is a placeholder (`MockDetectionEngine`: one sample detection per frame). The ML service (YOLO + OpenCV) should implement `DetectionEngine` and receive the real video, not just the preview frames.
- The browser extracts up to 6 preview frames from a video; the original video file is not uploaded yet.
- The token is kept in `localStorage` (simple for a college project). All text shown from the API is HTML-escaped; for a production site consider httpOnly cookies, rate limiting on login, and HTTPS.
- Frame pictures are served without login because `<img>` tags can't send a token. Their names are random and unguessable.
- The backend was written without Maven/PostgreSQL access, so it has not been compiled or run against a real database yet. Run `mvn spring-boot:run` first and fix anything it reports. The frontend API layer was tested against a fake server.
