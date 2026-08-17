package com.course.microservices.songservice.service;

import com.course.microservices.songservice.dto.SongDto;
import com.course.microservices.songservice.entity.SongEntity;
import com.course.microservices.songservice.exception.CsvTooLongException;
import com.course.microservices.songservice.exception.SongAlreadyExistsException;
import com.course.microservices.songservice.exception.SongNotFoundException;
import com.course.microservices.songservice.repo.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SongService {

    private static final int MAX_CSV_LENGTH = 200;
    private final SongRepository songRepository;

    public SongDto getSongById(Long id) {
        SongEntity songEntity = songRepository.findById(id)
                .orElseThrow(() -> new SongNotFoundException("Song metadata for ID=" + id + " not found"));
        return toDto(songEntity);
    }

    public Long createSong(SongDto songDto) {
        Long id = songDto.getId();
        if (songRepository.existsById(id)) {
            throw new SongAlreadyExistsException("Metadata for resource ID=" + id + " already exists");
        }
        return songRepository.save(toEntity(songDto)).getId();
    }


    public List<Long> deleteSongs(List<Long> ids, String rawCsv) {
        if (rawCsv != null && rawCsv.length() > MAX_CSV_LENGTH) {
            throw new CsvTooLongException(rawCsv.length());
        }
        List<Long> existingIds = songRepository
                .findAllById(ids)
                .stream()
                .map(SongEntity::getId)
                .toList();
        songRepository.deleteAllById(existingIds);
        return existingIds;
    }

    private SongDto toDto(SongEntity songEntity) {
        return new SongDto(
                songEntity.getId(),
                songEntity.getName(),
                songEntity.getArtist(),
                songEntity.getAlbum(),
                songEntity.getDuration(),
                songEntity.getYear()
        );
    }

    private SongEntity toEntity(SongDto songDto) {
        return new SongEntity(
                songDto.getId(),
                songDto.getName(),
                songDto.getArtist(),
                songDto.getAlbum(),
                songDto.getDuration(),
                songDto.getYear()
        );
    }

}
