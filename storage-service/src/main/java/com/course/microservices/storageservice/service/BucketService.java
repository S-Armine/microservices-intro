package com.course.microservices.storageservice.service;

import com.course.microservices.storageservice.exception.BucketProvisioningException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

@Service
@RequiredArgsConstructor
public class BucketService {

    private final S3Client s3Client;

    public void ensureBucketExists(String bucket) {
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (NoSuchBucketException ex) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            }
        } catch (SdkException ex) {
            throw new BucketProvisioningException("Failed to create bucket '" + bucket + "'", ex);
        }
    }
}
