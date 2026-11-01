package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.exception.ResourceStorageException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class S3StorageIT {

    @RegisterExtension
    static WireMockExtension s3 = WireMockExtension.newInstance().options(options().dynamicPort()).build();

    private final ResourceStorageService storage = new ResourceStorageService(S3Client.builder()
            .endpointOverride(URI.create(s3.baseUrl()))
            .forcePathStyle(true)
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("noop", "noop")))
            .build());

    @Test
    void uploadPutsTheBytesUnderBucketAndKey() {
        s3.stubFor(put(urlEqualTo("/resources/a.mp3")).willReturn(aResponse().withStatus(200)));

        storage.upload("resources", "a.mp3", new byte[]{1, 2, 3});

        s3.verify(putRequestedFor(urlEqualTo("/resources/a.mp3"))
                .withHeader("Content-Type", com.github.tomakehurst.wiremock.client.WireMock.equalTo("audio/mpeg")));
    }

    @Test
    void downloadReturnsTheStoredBytes() {
        s3.stubFor(get(urlEqualTo("/resources/b.mp3")).willReturn(aResponse().withStatus(200).withBody(new byte[]{7, 8, 9})));

        assertThat(storage.download("resources", "b.mp3")).containsExactly(7, 8, 9);
    }

    @Test
    void deleteRemovesTheObject() {
        s3.stubFor(delete(urlEqualTo("/resources/c.mp3")).willReturn(aResponse().withStatus(204)));

        storage.delete("resources", "c.mp3");

        s3.verify(deleteRequestedFor(urlEqualTo("/resources/c.mp3")));
    }

    @Test
    void missingObjectSurfacesAsStorageException() {
        s3.stubFor(get(urlEqualTo("/resources/missing.mp3")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> storage.download("resources", "missing.mp3"))
                .isInstanceOf(ResourceStorageException.class)
                .hasMessage("Failed to download resource from S3");
    }
}
