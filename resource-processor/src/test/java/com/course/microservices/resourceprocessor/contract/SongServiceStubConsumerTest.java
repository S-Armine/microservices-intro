package com.course.microservices.resourceprocessor.contract;

import com.course.microservices.resourceprocessor.dto.SongMetadataDto;
import com.course.microservices.resourceprocessor.service.SongServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.course.microservices:song-service:0.0.1-SNAPSHOT:stubs",
        stubsMode = StubRunnerProperties.StubsMode.REMOTE)
class SongServiceStubConsumerTest {

    @Autowired
    private SongServiceClient songServiceClient;

    @Test
    void sendsMetadataAcceptedByTheCreateSongContract() {
        SongMetadataDto metadata = new SongMetadataDto(1L, "Yesterday", "The Beatles", "Help!", "02:05", "1965");

        assertThatCode(() -> songServiceClient.sendMetadata(metadata)).doesNotThrowAnyException();
    }

    @Test
    void propagatesTheConflictDefinedByTheCreateSongConflictContract() {
        SongMetadataDto metadata = new SongMetadataDto(2L, "Yesterday", "The Beatles", "Help!", "02:05", "1965");

        assertThatThrownBy(() -> songServiceClient.sendMetadata(metadata))
                .isInstanceOf(HttpClientErrorException.Conflict.class);
    }
}
