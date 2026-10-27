package com.course.microservices.resourceprocessor.service;

import com.course.microservices.resourceprocessor.dto.SongMetadataDto;
import com.course.microservices.resourceprocessor.exception.InvalidMp3Exception;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceProcessingServiceTest {

    @Mock
    private ResourceServiceClient resourceServiceClient;
    @Mock
    private Mp3MetadataExtractorService extractor;
    @Mock
    private SongServiceClient songServiceClient;

    @InjectMocks
    private ResourceProcessingService service;

    @Test
    void sendsTheExtractedMetadataToSongServiceWithTheResourceId() {
        byte[] audio = {1, 2, 3};
        when(resourceServiceClient.fetchResourceData("17")).thenReturn(audio);
        when(extractor.extract(audio)).thenReturn(new SongMetadataDto(null, "Yesterday", "The Beatles", "Help!", "02:05", "1965"));

        service.processResource("17");

        verify(songServiceClient).sendMetadata(new SongMetadataDto(17L, "Yesterday", "The Beatles", "Help!", "02:05", "1965"));
    }

    @Test
    void doesNotCallSongServiceWhenTheFileIsNotAnMp3() {
        when(resourceServiceClient.fetchResourceData("5")).thenReturn(new byte[]{9});
        when(extractor.extract(any())).thenThrow(new InvalidMp3Exception("bad"));

        assertThatThrownBy(() -> service.processResource("5")).isInstanceOf(InvalidMp3Exception.class);

        verify(songServiceClient, never()).sendMetadata(any());
    }

    @Test
    void rejectsIdsThatAreNotNumbers() {
        when(resourceServiceClient.fetchResourceData("abc")).thenReturn(new byte[]{1});
        when(extractor.extract(any())).thenReturn(new SongMetadataDto());

        assertThatThrownBy(() -> service.processResource("abc")).isInstanceOf(NumberFormatException.class);

        verify(songServiceClient, never()).sendMetadata(any());
    }
}
