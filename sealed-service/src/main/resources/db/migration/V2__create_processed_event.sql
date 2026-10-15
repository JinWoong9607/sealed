CREATE TABLE processed_event (
    consumer        VARCHAR(100)    NOT NULL,
    event_id        UUID            NOT NULL,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, event_id)
);