-- BE-03: ledger status (Pix reserve/confirm/compensate), foreign key on the ledger account, transactional outbox.

-- 1. Ledger status. Existing rows are completed operations.
ALTER TABLE transaction ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE transaction ADD CONSTRAINT ck_transaction_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'));

-- 2. Mobile recharge used to store the account NUMBER in account_id (defect F-08). Repair such legacy rows
--    (disposable development data) before adding the foreign key.
UPDATE transaction t
   SET account_id = a.id
  FROM account a
 WHERE t.account_id = a.number
   AND NOT EXISTS (SELECT 1 FROM account x WHERE x.id = t.account_id);

ALTER TABLE transaction ADD CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) REFERENCES account (id);

-- 3. Transactional outbox: events are written in the same transaction as the business change and published
--    to Kafka by a relay (at-least-once).
CREATE TABLE outbox_event (
    id           VARCHAR(255) NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    event_key    VARCHAR(255),
    payload      TEXT         NOT NULL,
    payload_type VARCHAR(255),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   TEXT,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_event_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
