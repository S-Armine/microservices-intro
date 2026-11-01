package com.course.microservices.songservice.service;

import com.course.microservices.songservice.dto.SongDto;
import com.course.microservices.songservice.entity.SongEntity;
import com.course.microservices.songservice.exception.CsvTooLongException;
import com.course.microservices.songservice.exception.SongAlreadyExistsException;
import com.course.microservices.songservice.exception.SongNotFoundException;
import com.course.microservices.songservice.repo.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.course.microservices.songservice.helper.SongTestHelper.song;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService underTest;

    @Test
    void getSongByIdReturnsMappedDto() {
        when(songRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));

        assertThat(underTest.getSongById(1L)).isEqualTo(song(1L));
    }

    @Test
    void getSongByIdThrowsWhenMissing() {
        when(songRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> underTest.getSongById(5L))
                .isInstanceOf(SongNotFoundException.class)
                .hasMessage("Song metadata for ID=5 not found");
    }

    @Test
    void createSongSavesEntityAndReturnsId() {
        when(songRepository.existsById(1L)).thenReturn(false);
        when(songRepository.save(any(SongEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        SongDto dto = song(1L);

        assertThat(underTest.createSong(dto)).isEqualTo(1L);
        verify(songRepository).save(entity(1L));
    }

    @Test
    void createSongRejectsDuplicates() {
        when(songRepository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> underTest.createSong(song(1L)))
                .isInstanceOf(SongAlreadyExistsException.class)
                .hasMessage("Metadata for resource ID=1 already exists");
        verify(songRepository, never()).save(any());
    }

    @Test
    void deleteSongsOnlyDeletesExistingOnes() {
        when(songRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(entity(1L), entity(3L)));

        List<Long> deleted = underTest.deleteSongs(List.of(1L, 2L, 3L), "1,2,3");

        assertThat(deleted).containsExactly(1L, 3L);
        verify(songRepository).deleteAllById(List.of(1L, 3L));
    }

    @Test
    void deleteSongsReturnsEmptyListWhenNothingExists() {
        when(songRepository.findAllById(List.of(9L))).thenReturn(List.of());

        assertThat(underTest.deleteSongs(List.of(9L), "9")).isEmpty();
    }

    @Test
    void deleteSongsRejectsCsvLongerThanTheLimit() {
        String csv = "1".repeat(CsvTooLongException.MAX_CSV_LENGTH + 1);

        assertThatThrownBy(() -> underTest.deleteSongs(List.of(1L), csv))
                .isInstanceOf(CsvTooLongException.class)
                .hasMessageContaining("received 201 characters, maximum allowed is 200");
        verify(songRepository, never()).findAllById(any());
    }

    @Test
    void deleteSongsAcceptsMissingRawCsv() {
        when(songRepository.findAllById(List.of(1L))).thenReturn(List.of(entity(1L)));

        assertThat(underTest.deleteSongs(List.of(1L), null)).containsExactly(1L);
    }

    private static SongEntity entity(long id) {
        return new SongEntity(id, "Yesterday", "The Beatles", "Help!", "02:05", "1965");
    }
}
