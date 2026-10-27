package com.course.microservices.songservice.repo;

import com.course.microservices.songservice.entity.SongEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SongRepositoryIT {

    @Autowired
    private SongRepository repository;

    @Test
    void persistsAndReadsBackAllColumns() {
        repository.saveAndFlush(new SongEntity(10L, "Yesterday", "The Beatles", "Help!", "02:05", "1965"));

        SongEntity found = repository.findById(10L).orElseThrow();

        assertThat(found.getName()).isEqualTo("Yesterday");
        assertThat(found.getArtist()).isEqualTo("The Beatles");
        assertThat(found.getAlbum()).isEqualTo("Help!");
        assertThat(found.getDuration()).isEqualTo("02:05");
        assertThat(found.getYear()).isEqualTo("1965");
    }

    @Test
    void deleteAllByIdRemovesOnlyTheRequestedRows() {
        repository.saveAndFlush(new SongEntity(1L, "a", "b", "c", "01:00", "2000"));
        repository.saveAndFlush(new SongEntity(2L, "a", "b", "c", "01:00", "2000"));

        repository.deleteAllById(List.of(1L));

        assertThat(repository.findAll()).extracting(SongEntity::getId).containsExactly(2L);
    }
}
