package com.course.microservices.songservice.repo;

import com.course.microservices.songservice.entity.SongEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<SongEntity, Long> {
}
