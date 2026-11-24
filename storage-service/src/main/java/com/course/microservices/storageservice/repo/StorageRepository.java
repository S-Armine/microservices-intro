package com.course.microservices.storageservice.repo;

import com.course.microservices.storageservice.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StorageRepository extends JpaRepository<Storage, Long> {



    boolean existsByBucketAndPath(String bucket, String path);

}
