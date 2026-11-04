-- SL-03: Idempotency-Key for the four money POSTs. One row per (customer subject, key): the hash of the request body
-- tells a replay from a reuse with a different request; status is NULL while the first attempt is running and holds the
-- stored HTTP status once it finished. Rows are forgotten after 24 hours.
CREATE TABLE idempotency_key (
    subject      VARCHAR(100) NOT NULL,
    idem_key     VARCHAR(64)  NOT NULL,
    request_hash CHAR(64)     NOT NULL,
    status       SMALLINT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_idempotency_key PRIMARY KEY (subject, idem_key)
);
CREATE INDEX idx_idempotency_key_created_at ON idempotency_key (created_at);
