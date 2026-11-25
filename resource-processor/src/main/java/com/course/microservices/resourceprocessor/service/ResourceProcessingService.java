package com.course.microservices.resourceprocessor.service;

import com.course.microservices.resourceprocessor.producer.ResourceProcessedProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceProcessingService {

    private final ResourceServiceClient resourceServiceClient;
    private final Mp3MetadataExtractorService mp3MetadataExtractorService;
    private final SongServiceClient songServiceClient;
    private final ResourceProcessedProducer resourceProcessedProducer;

    public void processResource(String resourceId) {
        log.info("Processing resource ID={}", resourceId);
        byte[] resourceData = resourceServiceClient.fetchResourceData(resourceId);
        var metadata = mp3MetadataExtractorService.extract(resourceData);
        metadata.setId(Long.valueOf(resourceId));
        songServiceClient.sendMetadata(metadata);
        resourceProcessedProducer.sendMessage(resourceId);
        log.info("Resource ID={} processed, metadata sent to song-service", resourceId);
    }
}
