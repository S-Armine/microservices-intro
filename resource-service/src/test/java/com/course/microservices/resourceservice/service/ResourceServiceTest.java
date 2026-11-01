package com.course.microservices.resourceservice.service;

import com.course.microservices.resourceservice.entity.Resource;
import com.course.microservices.resourceservice.exception.CsvTooLongException;
import com.course.microservices.resourceservice.exception.ResourceNotFoundException;
import com.course.microservices.resourceservice.exception.ResourceStorageException;
import com.course.microservices.resourceservice.producer.ResourceProducer;
import com.course.microservices.resourceservice.repo.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private SongServiceClient songServiceClient;
    @Mock
    private ResourceStorageService storageService;
    @Mock
    private ResourceProducer producer;

    @InjectMocks
    private ResourceService resourceService;

    @BeforeEach
    void setBucket() {
        ReflectionTestUtils.setField(resourceService, "bucket", "resources");
    }

    @Test
    void uploadStoresTheFileThenTheRowThenPublishesTheId() {
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            saved.setId(11L);
            return saved;
        });
        byte[] audio = {1, 2, 3};

        Long id = resourceService.uploadResource(audio);

        assertThat(id).isEqualTo(11L);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(storageService, resourceRepository, producer);
        order.verify(storageService).upload(org.mockito.ArgumentMatchers.eq("resources"), key.capture(), org.mockito.ArgumentMatchers.eq(audio));
        order.verify(resourceRepository).save(any(Resource.class));
        order.verify(producer).sendMessage("11");
        assertThat(key.getValue()).startsWith("resources/").endsWith(".mp3");
    }

    @Test
    void uploadDoesNotSaveOrPublishWhenS3Fails() {
        doThrow(new ResourceStorageException("down", new RuntimeException()))
                .when(storageService).upload(anyString(), anyString(), any());

        assertThatThrownBy(() -> resourceService.uploadResource(new byte[]{1}))
                .isInstanceOf(ResourceStorageException.class);

        verifyNoInteractions(resourceRepository, producer);
    }

    @Test
    void getResourceReadsTheFileFromTheStoredLocation() {
        Resource resource = new Resource(3L, "resources", "resources/a.mp3", LocalDateTime.now());
        when(resourceRepository.findById(3L)).thenReturn(Optional.of(resource));
        when(storageService.download("resources", "resources/a.mp3")).thenReturn(new byte[]{9});

        assertThat(resourceService.getResourceById(3L)).containsExactly(9);
    }

    @Test
    void getResourceThrowsWhenUnknown() {
        when(resourceRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResourceById(3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource with ID=3 not found");
    }

    @Test
    void deleteRemovesMetadataThenFileThenRowForExistingIdsOnly() {
        Resource first = new Resource(1L, "resources", "k1", LocalDateTime.now());
        Resource second = new Resource(2L, "resources", "k2", LocalDateTime.now());
        when(resourceRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(first, second));

        List<Long> deleted = resourceService.deleteResources(List.of(1L, 2L, 3L), "1,2,3");

        assertThat(deleted).containsExactly(1L, 2L);
        InOrder order = inOrder(songServiceClient, storageService, resourceRepository);
        order.verify(songServiceClient).deleteMetadata(List.of(1L, 2L));
        order.verify(storageService).delete("resources", "k1");
        order.verify(resourceRepository).deleteById(1L);
        order.verify(storageService).delete("resources", "k2");
        order.verify(resourceRepository).deleteById(2L);
    }

    @Test
    void deleteDoesNothingWhenNoIdExists() {
        when(resourceRepository.findAllById(List.of(7L))).thenReturn(List.of());

        assertThat(resourceService.deleteResources(List.of(7L), "7")).isEmpty();

        verifyNoInteractions(songServiceClient, storageService);
        verify(resourceRepository, never()).deleteById(any());
    }

    @Test
    void deleteKeepsTheFileWhenSongServiceFails() {
        Resource resource = new Resource(1L, "resources", "k1", LocalDateTime.now());
        when(resourceRepository.findAllById(List.of(1L))).thenReturn(List.of(resource));
        doThrow(new IllegalStateException("song-service down")).when(songServiceClient).deleteMetadata(any());

        assertThatThrownBy(() -> resourceService.deleteResources(List.of(1L), "1"))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(storageService);
        verify(resourceRepository, never()).deleteById(any());
    }

    @Test
    void deleteRejectsCsvLongerThanTheLimit() {
        String csv = "1".repeat(CsvTooLongException.MAX_CSV_LENGTH + 1);

        assertThatThrownBy(() -> resourceService.deleteResources(List.of(1L), csv))
                .isInstanceOf(CsvTooLongException.class);

        verifyNoInteractions(resourceRepository);
    }
}
