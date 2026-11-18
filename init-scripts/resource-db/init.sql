CREATE TABLE IF NOT EXISTS resources (
     id         BIGSERIAL PRIMARY KEY,
     s3_bucket  VARCHAR(255) NOT NULL,
     s3_key     VARCHAR(1024) NOT NULL UNIQUE,
     state      VARCHAR(32)  NOT NULL DEFAULT 'STAGING',
     created_at TIMESTAMP    NOT NULL
);
