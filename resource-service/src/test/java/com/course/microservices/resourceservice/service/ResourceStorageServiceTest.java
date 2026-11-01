package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.exception.ResourceStorageException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceStorageServiceTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final ResourceStorageService storage = new ResourceStorageService(s3Client);

    @Test
    void uploadSendsAnAudioMpegObject() throws java.io.IOException {
        storage.upload("bucket", "key.mp3", new byte[]{1, 2});

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(request.capture(), body.capture());
        assertThat(request.getValue().bucket()).isEqualTo("bucket");
        assertThat(request.getValue().key()).isEqualTo("key.mp3");
        assertThat(request.getValue().contentType()).isEqualTo("audio/mpeg");
        try (var stream = body.getValue().contentStreamProvider().newStream()) {
            assertThat(stream.readAllBytes()).containsExactly(1, 2);
        }
    }

    @Test
    void downloadReturnsTheObjectBytes() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[]{5, 6}));

        assertThat(storage.download("bucket", "key.mp3")).containsExactly(5, 6);
    }

    @Test
    void deleteRemovesTheObject() {
        storage.delete("bucket", "key.mp3");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(request.capture());
        assertThat(request.getValue().bucket()).isEqualTo("bucket");
        assertThat(request.getValue().key()).isEqualTo("key.mp3");
    }

    @Test
    void sdkFailuresAreWrapped() {
        SdkClientException failure = SdkClientException.create("no route to s3");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(failure);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(failure);
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(failure);

        assertThatThrownBy(() -> storage.upload("b", "k", new byte[0]))
                .isInstanceOf(ResourceStorageException.class).hasMessage("Failed to upload resource to S3").hasCause(failure);
        assertThatThrownBy(() -> storage.download("b", "k"))
                .isInstanceOf(ResourceStorageException.class).hasMessage("Failed to download resource from S3");
        assertThatThrownBy(() -> storage.delete("b", "k"))
                .isInstanceOf(ResourceStorageException.class).hasMessage("Failed to delete resource from S3");
    }
}
