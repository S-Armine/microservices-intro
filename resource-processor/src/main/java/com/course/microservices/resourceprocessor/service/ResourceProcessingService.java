package com.course.microservices.resourceprocessor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceProcessingService {

    private final ResourceServiceClient resourceServiceClient;
    private final Mp3MetadataExtractorService mp3MetadataExtractorService;
    private final SongServiceClient songServiceClient;

    public void processResource(String resourceId) {
        byte[] resourceData = resourceServiceClient.fetchResourceData(resourceId);
        var metadata = mp3MetadataExtractorService.extract(resourceData);
        metadata.setId(Long.valueOf(resourceId));
        songServiceClient.sendMetadata(metadata);
    }
}
