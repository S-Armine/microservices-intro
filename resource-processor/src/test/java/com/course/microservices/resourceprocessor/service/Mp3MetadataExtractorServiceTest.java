package com.course.microservices.resourceprocessor.service;

import com.course.microservices.resourceprocessor.dto.SongMetadataDto;
import com.course.microservices.resourceprocessor.exception.InvalidMp3Exception;
import com.course.microservices.resourceprocessor.helper.Mp3Fixtures;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Mp3MetadataExtractorServiceTest {

    private final Mp3MetadataExtractorService extractor = new Mp3MetadataExtractorService();

    @Test
    void readsTagsAndFormatsTheDurationAsMinutesAndSeconds() {
        SongMetadataDto metadata = extractor.extract(Mp3Fixtures.song());

        assertThat(metadata.getId()).isNull();
        assertThat(metadata.getName()).isEqualTo("Yesterday");
        assertThat(metadata.getArtist()).isEqualTo("The Beatles");
        assertThat(metadata.getAlbum()).isEqualTo("Help!");
        assertThat(metadata.getYear()).isEqualTo("1965");
        assertThat(metadata.getDuration()).isEqualTo("00:03");
    }

    @Test
    void longerTracksRollOverToMinutes() {
        SongMetadataDto metadata = extractor.extract(Mp3Fixtures.mp3("t", "a", "b", "2001", 2800));

        assertThat(metadata.getDuration()).isEqualTo("01:13");
    }

    @Test
    void bytesThatAreNotAnMp3AreRejected() {
        byte[] text = "this is definitely not audio".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract(text))
                .isInstanceOf(InvalidMp3Exception.class)
                .hasMessage("The request body is invalid MP3");
    }

    @Test
    void emptyInputIsRejected() {
        assertThatThrownBy(() -> extractor.extract(new byte[0])).isInstanceOf(InvalidMp3Exception.class);
    }
}
