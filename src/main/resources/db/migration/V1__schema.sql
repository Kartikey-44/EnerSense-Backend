-- Enersense initial schema.
-- Flyway applies this to an empty database on first start, so the project is
-- reproducible from a clean checkout with no manual SQL step.

CREATE TABLE consumer (
    consumer_id VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    NOT NULL
);

CREATE TABLE meter (
    meter_id    VARCHAR(20) PRIMARY KEY,
    consumer_id VARCHAR(20) NOT NULL REFERENCES consumer (consumer_id),
    meter_type  VARCHAR(50)
);

CREATE TABLE meter_reading (
    reading_id            BIGSERIAL     PRIMARY KEY,
    meter_id              VARCHAR(20)   NOT NULL REFERENCES meter (meter_id),
    reading_timestamp     TIMESTAMP     NOT NULL,
    energy_consumption_wh NUMERIC(12, 3) NOT NULL
);

-- The cursor and every query that respects it order by (timestamp, reading_id)
-- within one meter. Without this index all of them degrade to a sequential scan.
CREATE INDEX idx_reading_meter_ts ON meter_reading (meter_id, reading_timestamp, reading_id);

-- A meter cannot have two readings for the same instant. This is what makes the
-- data loader safe to re-run.
CREATE UNIQUE INDEX uq_reading_meter_ts ON meter_reading (meter_id, reading_timestamp);
