CREATE TABLE IF NOT EXISTS resources (
     id         BIGSERIAL PRIMARY KEY,
     audio_data BYTEA        NOT NULL,
     created_at TIMESTAMP    NOT NULL
);
