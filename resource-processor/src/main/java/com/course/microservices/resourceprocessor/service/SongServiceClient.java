package com.course.microservices.resourceprocessor.service;

import com.course.microservices.resourceprocessor.dto.SongMetadataDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class SongServiceClient {

    private final RestClient restClient;

    public SongServiceClient(@Qualifier("songServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(
            includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2,
            delay = 1000,
            multiplier = 2
    )
    public void sendMetadata(SongMetadataDto metadata) {
        restClient.post()
                .body(metadata)
                .retrieve()
                .toBodilessEntity();
    }
}
