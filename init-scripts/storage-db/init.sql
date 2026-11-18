CREATE TABLE IF NOT EXISTS storages (
    id           BIGSERIAL     PRIMARY KEY,
    storage_type VARCHAR(32)   NOT NULL,
    bucket       VARCHAR(255)  NOT NULL,
    path         VARCHAR(1024) NOT NULL,
    CONSTRAINT uq_storages_bucket_path UNIQUE (bucket, path)
);

-- Bucket names must match the ones created in init-scripts/localstack/init-s3.sh
INSERT INTO storages (storage_type, bucket, path) VALUES
    ('STAGING',   'staging-bucket',   'resources'),
    ('PERMANENT', 'permanent-bucket', 'resources')
ON CONFLICT (bucket, path) DO NOTHING;
