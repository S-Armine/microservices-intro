package com.course.microservices.storageservice.service;

import com.course.microservices.storageservice.dto.StorageDto;
import com.course.microservices.storageservice.dto.StorageRequest;
import com.course.microservices.storageservice.entity.Storage;
import com.course.microservices.storageservice.exception.CsvTooLongException;
import com.course.microservices.storageservice.exception.StorageAlreadyExistsException;
import com.course.microservices.storageservice.repo.StorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.course.microservices.storageservice.exception.CsvTooLongException.MAX_CSV_LENGTH;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageRepository storageRepository;
    private final BucketService bucketService;

    public List<StorageDto> getStorages() {
        return storageRepository.findAll().stream().map(StorageDto::from).toList();
    }

    public Long createStorage(StorageRequest request) {
        String path = request.path().trim().replaceAll("^/+|/+$", "");
        if (storageRepository.existsByBucketAndPath(request.bucket(), path)) {
            throw new StorageAlreadyExistsException(request.bucket(), path);
        }
        bucketService.ensureBucketExists(request.bucket());
        return storageRepository.save(new Storage(null, request.storageType(), request.bucket(), path)).getId();
    }

    public List<Long> deleteStorages(List<Long> ids, String rawCsv) {
        if (rawCsv != null && rawCsv.length() > MAX_CSV_LENGTH) {
            throw new CsvTooLongException(rawCsv.length());
        }
        List<Long> existingIds = storageRepository.findAllById(ids).stream().map(Storage::getId).toList();
        storageRepository.deleteAllById(existingIds);
        return existingIds;
    }
}
