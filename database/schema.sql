-- RoadGuard AI - PostgreSQL schema
-- The backend creates these tables automatically (spring.jpa.hibernate.ddl-auto=update).
-- Use this file if you prefer to create them yourself:
--   createdb roadguard && psql -d roadguard -f database/schema.sql
-- ...and then start the backend with DDL_AUTO=validate.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(255)  NOT NULL UNIQUE,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(20)   NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE uploads (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT        NOT NULL REFERENCES users(id),
    name          VARCHAR(150)  NOT NULL,
    media_kind    VARCHAR(10)   NOT NULL CHECK (media_kind IN ('VIDEO', 'PHOTO')),
    file_name     VARCHAR(255),
    gps_file_name VARCHAR(255),
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE upload_frames (
    upload_id BIGINT       NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
    position  INTEGER      NOT NULL,
    image_url VARCHAR(300) NOT NULL,
    time_sec  DOUBLE PRECISION,
    PRIMARY KEY (upload_id, position)
);

CREATE TABLE detections (
    id           BIGSERIAL PRIMARY KEY,
    upload_id    BIGINT      NOT NULL REFERENCES uploads(id),
    seq          INTEGER     NOT NULL,
    frame_index  INTEGER     NOT NULL,
    frame_number INTEGER     NOT NULL,
    time_label   VARCHAR(10),
    confidence   INTEGER     NOT NULL,
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    status       VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    box_x        INTEGER     NOT NULL,
    box_y        INTEGER     NOT NULL,
    box_w        INTEGER     NOT NULL,
    box_h        INTEGER     NOT NULL
);
CREATE INDEX idx_detections_upload ON detections(upload_id);

CREATE TABLE complaints (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id),
    description VARCHAR(2000) NOT NULL,
    severity    VARCHAR(10)   NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED')),
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ
);

-- One complaint can cover many detections; a detection can be in only one complaint.
CREATE TABLE complaint_detections (
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    detection_id BIGINT NOT NULL REFERENCES detections(id),
    PRIMARY KEY (complaint_id, detection_id),
    CONSTRAINT uq_complaint_detection UNIQUE (detection_id)
);
