package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.SongMetadataDto;
import com.course.microservices.resourceservice.exception.SongServiceInvocationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongServiceClient {

    private final RestClient songServiceRestClient;

    public void sendMetadata(SongMetadataDto metadata) {
        try {
            songServiceRestClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(metadata)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException | ResourceAccessException ex) {
            throw new SongServiceInvocationException("Failed to save song metadata in song service", ex);
        }
    }

    public void deleteMetadata(List<Long> resourceIds) {
        songServiceRestClient.delete()
                .uri(uriBuilder -> uriBuilder.queryParam("id", resourceIds).build())
                .retrieve()
                .toBodilessEntity();
    }
}