package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.StorageDto;
import com.course.microservices.resourceservice.entity.Resource;
import com.course.microservices.resourceservice.entity.ResourceState;
import com.course.microservices.resourceservice.exception.CsvTooLongException;
import com.course.microservices.resourceservice.exception.ResourceNotFoundException;
import com.course.microservices.resourceservice.producer.ResourceProducer;
import com.course.microservices.resourceservice.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.course.microservices.resourceservice.exception.CsvTooLongException.MAX_CSV_LENGTH;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String STAGING = "STAGING";
    private static final String PERMANENT = "PERMANENT";

    private final ResourceRepository resourceRepository;
    private final SongServiceClient songServiceClient;
    private final ResourceStorageService resourceStorageService;
    private final ResourceProducer resourceProducer;
    private final StorageServiceClient storageServiceClient;

    public byte[] getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));
        return resourceStorageService.download(resource.getS3Bucket(), resource.getS3Key());
    }

    public List<Long> deleteResources(List<Long> ids, String rawCsv) {
        if (rawCsv != null && rawCsv.length() > MAX_CSV_LENGTH) {
            throw new CsvTooLongException(rawCsv.length());
        }
        List<Resource> resources = resourceRepository.findAllById(ids);
        List<Long> existingIds = resources
                .stream()
                .map(Resource::getId)
                .toList();
        if (existingIds.isEmpty()) {
            return existingIds;
        }
        songServiceClient.deleteMetadata(existingIds);
        for (Resource resource : resources) {
            resourceStorageService.delete(resource.getS3Bucket(), resource.getS3Key());
            resourceRepository.deleteById(resource.getId());
        }
        return existingIds;
    }

    public Long uploadResource(byte[] audioData) {
        StorageDto storage = storageServiceClient.getStorageByType(STAGING);
        String key = storage.path() + "/" + UUID.randomUUID() + ".mp3";
        resourceStorageService.upload(storage.bucket(), key, audioData);
        Resource savedResource = resourceRepository.save(new Resource(null, storage.bucket(), key, ResourceState.STAGING, LocalDateTime.now()));
        resourceProducer.sendMessage(savedResource.getId().toString());
        return savedResource.getId();
    }

    public void moveToPermanent(Long id) {
        Resource resource = resourceRepository.findById(id).orElse(null);
        if (resource == null) {
            log.warn("Resource with ID={} not found, skipping move to {}", id, PERMANENT);
            return;
        }
        if (resource.getState() == ResourceState.PERMANENT) {
            return;
        }
        StorageDto storage = storageServiceClient.getStorageByType(PERMANENT);
        String key = storage.path() + "/" + resource.getS3Key().substring(resource.getS3Key().lastIndexOf('/') + 1);
        String sourceBucket = resource.getS3Bucket();
        String sourceKey = resource.getS3Key();
        resourceStorageService.copy(sourceBucket, sourceKey, storage.bucket(), key);
        resource.setState(ResourceState.PERMANENT);
        resource.setS3Bucket(storage.bucket());
        resource.setS3Key(key);
        resourceRepository.save(resource);
        resourceStorageService.delete(sourceBucket, sourceKey);
    }
}
