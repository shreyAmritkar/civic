CREATE TABLE complaint_media (
    id                UUID PRIMARY KEY,
    complaint_id      UUID         NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    uploaded_by       UUID         NOT NULL REFERENCES users(id),
    object_key        VARCHAR(512) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL CHECK (size_bytes > 0),
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    uploaded_at       TIMESTAMPTZ
);
CREATE INDEX idx_complaint_media_complaint ON complaint_media(complaint_id);