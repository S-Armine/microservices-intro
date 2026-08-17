package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.dto.SongMetadataDto;
import com.course.microservices.resourceservice.entity.Resource;
import com.course.microservices.resourceservice.exception.CsvTooLongException;
import com.course.microservices.resourceservice.exception.ResourceNotFoundException;
import com.course.microservices.resourceservice.repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.course.microservices.resourceservice.exception.CsvTooLongException.MAX_CSV_LENGTH;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final Mp3MetadataExtractorService mp3MetadataExtractorService;
    private final SongServiceClient songServiceClient;

    public byte[] getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource with ID=" + id + " not found"));
        return resource.getAudioData();
    }

    public List<Long> deleteResources(List<Long> ids, String rawCsv) {
        if (rawCsv != null && rawCsv.length() > MAX_CSV_LENGTH) {
            throw new CsvTooLongException(rawCsv.length());
        }
        List<Long> existingIds = resourceRepository
                .findAllById(ids)
                .stream()
                .map(Resource::getId)
                .toList();
        resourceRepository.deleteAllById(existingIds);
        songServiceClient.deleteMetadata(existingIds);
        return existingIds;
    }

    public Long uploadResource(byte[] audioData) {
        SongMetadataDto metadata = mp3MetadataExtractorService.extract(audioData);

        Resource savedResource = resourceRepository.save(new Resource(
                null,
                audioData,
                LocalDateTime.now()
        ));
        metadata.setId(savedResource.getId());

        try {
            songServiceClient.sendMetadata(metadata);
        } catch (Exception ex) {
            resourceRepository.deleteById(savedResource.getId());
            throw ex;
        }

        return savedResource.getId();
    }
}