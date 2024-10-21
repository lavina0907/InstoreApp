CREATE TABLE IF NOT EXISTS inventory_activity
(
    activity_id        SERIAL PRIMARY KEY,
    activity_type      VARCHAR(100)   NOT NULL,
    activity_value     VARCHAR(100) NOT NULL,
    message            TEXT,
    activity_timestamp TIMESTAMP,
    creation_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)