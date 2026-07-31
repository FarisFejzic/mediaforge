-- Users: account + auth. Roles support future admin/multi-tenant work.
CREATE TABLE users (
                       id            UUID PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(50)  NOT NULL DEFAULT 'USER',
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Uploads: one uploaded source file. storage_key points into MinIO.
CREATE TABLE uploads (
                         id            UUID PRIMARY KEY,
                         user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         original_name VARCHAR(512) NOT NULL,
                         media_type    VARCHAR(20)  NOT NULL,   -- IMAGE | VIDEO | AUDIO
                         size_bytes    BIGINT       NOT NULL,
                         storage_key   VARCHAR(512) NOT NULL,
                         status        VARCHAR(20)  NOT NULL,   -- e.g. RECEIVED | PROCESSING | DONE
                         created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Jobs: a unit of processing work; many per upload.
CREATE TABLE jobs (
                      id            UUID PRIMARY KEY,
                      upload_id     UUID         NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
                      type          VARCHAR(50)  NOT NULL,   -- THUMBNAIL | TRANSCODE | METADATA ...
                      status        VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
                      attempts      INT          NOT NULL DEFAULT 0,
                      max_attempts  INT          NOT NULL DEFAULT 3,     -- retry cap is per-job data
                      error         TEXT,                                -- populated on FAILED
                      next_retry_at TIMESTAMPTZ,                         -- backoff eligibility
                      started_at    TIMESTAMPTZ,
                      finished_at   TIMESTAMPTZ
);

-- Assets: derived outputs. One job produces one or more.
CREATE TABLE assets (
                        id            UUID PRIMARY KEY,
                        job_id        UUID         NOT NULL REFERENCES jobs(id)    ON DELETE CASCADE,
                        upload_id     UUID         NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
                        kind          VARCHAR(50)  NOT NULL,   -- THUMBNAIL | TRANSCODE | WAVEFORM ...
                        storage_key   VARCHAR(512) NOT NULL,
                        size_bytes    BIGINT       NOT NULL,
                        metadata_json JSONB,                   -- flexible per-asset metadata
                        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indexes on the foreign keys
CREATE INDEX idx_uploads_user_id ON uploads(user_id);
CREATE INDEX idx_jobs_upload_id  ON jobs(upload_id);
CREATE INDEX idx_assets_job_id   ON assets(job_id);
CREATE INDEX idx_assets_upload_id ON assets(upload_id);