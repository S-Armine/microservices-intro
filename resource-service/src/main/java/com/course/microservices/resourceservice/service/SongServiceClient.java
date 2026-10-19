package com.course.microservices.resourceservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongServiceClient {

    private final RestClient songServiceRestClient;

    @Retryable(
            includes = RestClientException.class,
            maxRetries = 2,
            delay = 1000,
            multiplier = 2
    )
    public void deleteMetadata(List<Long> resourceIds) {
        songServiceRestClient.delete()
                .uri(uriBuilder -> uriBuilder.queryParam("id", resourceIds).build())
                .retrieve()
                .toBodilessEntity();
    }
}