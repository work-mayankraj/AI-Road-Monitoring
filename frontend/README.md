# RoadGuard AI — Frontend Prototype

Frontend for the **RoadGuard AI** college project (Pothole Detection + Civic Complaints).

## System Architecture

```
Citizen Website (Uploads · Detections · Complaints)
        ↓
Spring Boot REST API (Authentication · Business logic · Admin APIs)
        ↓
PostgreSQL Database (Users · Uploads · Detections · Complaints · Status history)
        ↑↓
Administrator Dashboard (Live data · Verification · Complaint management)
```

### Technology Stack

| Layer              | Technology                          |
|--------------------|-------------------------------------|
| Frontend           | HTML + Tailwind CSS + Leaflet       |
| Backend (planned)  | Spring Boot REST API                |
| Database           | PostgreSQL                          |
| AI / ML            | Python + YOLO + OpenCV              |
| Maps               | Leaflet + OpenStreetMap             |

> **Current status**: Only the **Frontend** is implemented (static prototype with mock data).  
> Backend (Spring Boot) and Database are planned as next steps.

## Pages

| Page              | File                  | Description                              |
|-------------------|-----------------------|------------------------------------------|
| Home              | `index.html`          | Landing page                             |
| Architecture      | `architecture.html`   | System architecture diagram              |
| Login             | `login.html`          | Separate User / Administrator sign-in    |
| Register          | `register.html`       | New account registration                 |
| Dashboard         | `dashboard.html`      | User dashboard & sessions                |
| Upload            | `upload.html`         | Video + GPS, or Photo + location upload  |
| Processing        | `processing.html`     | Detection results + Confirm/Reject       |
| Map               | `map.html`            | Interactive pothole map                  |
| Pothole Details   | `pothole-details.html`| Single pothole view                      |
| Complaints        | `complaint.html`      | Create & track complaints                |
| Admin             | `admin.html`          | Admin panel (status updates)             |

## How to Run

```bash
cd roadguard-ai-frontend
npx serve .
```

Then open: **http://localhost:3000**

## Demo Accounts

- **User** tab: `user@demo.com` / `demo123` → Dashboard
- **Administrator** tab: `admin@demo.com` / `admin123` → Admin Panel

Public registration always creates a User account. Users cannot open `admin.html`; admins are redirected away from user pages.

## Shared logic

`app.js` holds auth, uploads, detections, complaints and the map layers (stored in `localStorage`). The app starts empty: sessions, detections and complaints only appear after you upload and create them. Complaints created by a user show up in the admin panel, and admin status changes show up on the user's Complaints page.

## Project Structure (Planned Full Stack)

```
roadguard-ai/
├── frontend/                 ← Current folder (this project)
├── backend/                  ← Spring Boot (to be built)
│   ├── src/main/java/...
│   └── pom.xml
├── ml-service/               ← Python YOLO service (to be built)
│   ├── detect.py
│   └── requirements.txt
└── database/
    └── schema.sql
```

## Notes

- Data is mock/seed data saved in the browser (`localStorage`). Role checks are client-side only, so they are not real security; the Spring Boot backend must enforce roles.
- No real backend or AI is connected yet.
- Architecture page shows the planned full system design.


## Complaint flow
1. Upload a video (+ GPX/CSV) or a JPEG photo (+ location).
2. On the Processing page click a frame to enlarge it, then Confirm or Reject.
3. Tick **Select** on the confirmed frames you want and press the single **Create Complaint** button. One complaint can include several frames.

## Maps
Leaflet with Esri (ArcGIS) street and satellite tiles plus CARTO light tiles, switchable in the map's layer control. No API key is needed.
