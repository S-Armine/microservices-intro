package com.course.microservices.resourceservice.repo;

import com.course.microservices.resourceservice.entity.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResourceRepositoryIT {

    @Autowired
    private ResourceRepository repository;

    @Test
    void generatesAnIdAndStoresTheLocationOfTheFile() {
        Resource saved = repository.saveAndFlush(new Resource(null, "resources", "resources/a.mp3", LocalDateTime.now()));

        assertThat(saved.getId()).isNotNull();
        Resource found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getS3Bucket()).isEqualTo("resources");
        assertThat(found.getS3Key()).isEqualTo("resources/a.mp3");
    }

    @Test
    void theSameS3KeyCannotBeStoredTwice() {
        repository.saveAndFlush(new Resource(null, "resources", "resources/dup.mp3", LocalDateTime.now()));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new Resource(null, "resources", "resources/dup.mp3", LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
