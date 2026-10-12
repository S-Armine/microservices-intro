package com.course.microservices.resourceservice.repo;

import com.course.microservices.resourceservice.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
