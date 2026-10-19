#!/bin/sh
set -eu

if ! awslocal s3api head-bucket --bucket "$RESOURCE_S3_BUCKET" 2>/dev/null; then
    awslocal s3api create-bucket --bucket "$RESOURCE_S3_BUCKET"
fi
