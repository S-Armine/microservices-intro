package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.exception.ResourceStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class ResourceStorageService {

    private final S3Client s3Client;

    public void upload(String bucket, String key, byte[] audioData) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("audio/mpeg")
                    .build(), RequestBody.fromBytes(audioData));
        } catch (SdkException ex) {
            throw new ResourceStorageException("Failed to upload resource to S3", ex);
        }
    }

    public byte[] download(String bucket, String key) {
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()).asByteArray();
        } catch (SdkException ex) {
            throw new ResourceStorageException("Failed to download resource from S3", ex);
        }
    }

    public void delete(String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (SdkException ex) {
            throw new ResourceStorageException("Failed to delete resource from S3", ex);
        }
    }
}
