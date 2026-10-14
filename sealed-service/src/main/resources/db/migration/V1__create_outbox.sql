CREATE TABLE outbox (
                        id           BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY ,
                        event_id     UUID         NOT NULL UNIQUE,
                        aggregate_id VARCHAR(64)  NOT NULL,
                        event_type   VARCHAR(100) NOT NULL,
                        topic        VARCHAR(100) NOT NULL,
                        payload      JSONB        NOT NULL,
                        occurred_at  TIMESTAMPTZ  NOT NULL,
                        published_at TIMESTAMPTZ,
                        attempts     INT          NOT NULL DEFAULT 0,
                        last_error   TEXT
);

CREATE INDEX idx_outbox_unpublished ON outbox (id) WHERE published_at IS NULL;