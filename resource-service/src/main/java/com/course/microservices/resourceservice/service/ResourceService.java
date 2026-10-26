package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.entity.Resource;
import com.course.microservices.resourceservice.exception.CsvTooLongException;
import com.course.microservices.resourceservice.exception.ResourceNotFoundException;
import com.course.microservices.resourceservice.producer.ResourceProducer;
import com.course.microservices.resourceservice.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.course.microservices.resourceservice.exception.CsvTooLongException.MAX_CSV_LENGTH;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final SongServiceClient songServiceClient;
    private final ResourceStorageService resourceStorageService;
    private final ResourceProducer resourceProducer;

    @Value("${aws.s3.bucket}")
    private String bucket;

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
        String key = "resources/" + UUID.randomUUID() + ".mp3";
        resourceStorageService.upload(bucket, key, audioData);
        Resource savedResource = resourceRepository.save(new Resource(null, bucket, key, LocalDateTime.now()));
        resourceProducer.sendMessage(savedResource.getId().toString());
        return savedResource.getId();
    }
}