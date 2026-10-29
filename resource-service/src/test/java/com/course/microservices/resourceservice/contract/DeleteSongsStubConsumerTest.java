package com.course.microservices.resourceservice.contract;

import com.course.microservices.resourceservice.service.SongServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.course.microservices:song-service:0.0.1-SNAPSHOT:stubs",
        stubsMode = StubRunnerProperties.StubsMode.REMOTE)
class DeleteSongsStubConsumerTest {

    @Autowired
    private SongServiceClient songServiceClient;

    @Test
    void deletesTheSongMetadataAccordingToTheDeleteSongsContract() {
        assertThatCode(() -> songServiceClient.deleteMetadata(List.of(1L))).doesNotThrowAnyException();
    }
}
