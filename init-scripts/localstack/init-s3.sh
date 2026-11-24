#!/bin/sh
set -eu

for bucket in staging-bucket permanent-bucket; do
    if ! awslocal s3api head-bucket --bucket "$bucket" 2>/dev/null; then
        awslocal s3api create-bucket --bucket "$bucket"
    fi
done
