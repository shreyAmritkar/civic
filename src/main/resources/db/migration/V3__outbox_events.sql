CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY,
    type          VARCHAR(50)  NOT NULL,
    recipient_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    complaint_id  UUID         REFERENCES complaints(id) ON DELETE CASCADE,
    subject       VARCHAR(200) NOT NULL,
    body          TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts      INT          NOT NULL DEFAULT 0,
    last_error    TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    dispatched_at TIMESTAMPTZ,
    sent_at       TIMESTAMPTZ
);
-- the dispatcher only ever looks at rows that are not finished
CREATE INDEX idx_outbox_open ON outbox_events(created_at) WHERE status IN ('PENDING', 'DISPATCHED');