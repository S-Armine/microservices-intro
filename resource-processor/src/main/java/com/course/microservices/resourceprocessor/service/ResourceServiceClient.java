package com.course.microservices.resourceprocessor.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class ResourceServiceClient {

    private final RestClient restClient;

    public ResourceServiceClient(@Qualifier("resourceServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(
            includes = {ResourceAccessException.class, HttpServerErrorException.class},
            maxRetries = 2,
            delay = 1000,
            multiplier = 2
    )
    public byte[] fetchResourceData(String resourceId) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/{id}").build(resourceId))
                .retrieve()
                .body(byte[].class);
    }
}
