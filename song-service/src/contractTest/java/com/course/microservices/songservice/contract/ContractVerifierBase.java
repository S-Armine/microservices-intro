package com.course.microservices.songservice.contract;

import com.course.microservices.songservice.advice.SongServiceExceptionHandler;
import com.course.microservices.songservice.controller.SongController;
import com.course.microservices.songservice.dto.SongDto;
import com.course.microservices.songservice.exception.SongAlreadyExistsException;
import com.course.microservices.songservice.service.SongService;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ContractVerifierBase {

    @BeforeEach
    void setUp() {
        SongService songService = mock(SongService.class);
        when(songService.createSong(argThat(song -> song != null && song.getId() == 1L)))
                .thenReturn(1L);
        when(songService.createSong(argThat(song -> song != null && song.getId() == 2L)))
                .thenThrow(new SongAlreadyExistsException("Metadata for resource ID=2 already exists"));
        when(songService.deleteSongs(eq(List.of(1L)), eq("1"))).thenReturn(List.of(1L));

        standaloneSetup(new SongController(songService), new SongServiceExceptionHandler());
    }
}
