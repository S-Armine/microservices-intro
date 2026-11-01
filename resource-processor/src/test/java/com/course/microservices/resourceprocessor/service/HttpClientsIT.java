package com.course.microservices.resourceprocessor.service;

import com.course.microservices.resourceprocessor.dto.SongMetadataDto;
import com.course.microservices.resourceprocessor.helper.ProcessorHttpMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class HttpClientsIT {

    @Autowired
    private ResourceServiceClient resourceServiceClient;
    @Autowired
    private SongServiceClient songServiceClient;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        ProcessorHttpMocks.registerProperties(registry);
    }

    @BeforeEach
    void reset() {
        ProcessorHttpMocks.reset();
    }

    @Test
    void fetchesTheBytesOfAResource() {
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/7"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "audio/mpeg").withBody(new byte[]{1, 2, 3})));

        assertThat(resourceServiceClient.fetchResourceData("7")).containsExactly(1, 2, 3);
    }

    @Test
    void resourceFetchIsRetriedOnServerErrorsAndEventuallySucceeds() {
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/8")).inScenario("flaky")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503)).willSetStateTo("recovered"));
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/8")).inScenario("flaky")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse().withStatus(200).withBody(new byte[]{5})));

        assertThat(resourceServiceClient.fetchResourceData("8")).containsExactly(5);
        ProcessorHttpMocks.RESOURCE_SERVICE.verify(2, com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlEqualTo("/resources/8")));
    }

    @Test
    void clientErrorsAreNotRetried() {
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/9")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> resourceServiceClient.fetchResourceData("9"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);

        ProcessorHttpMocks.RESOURCE_SERVICE.verify(1, com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlEqualTo("/resources/9")));
    }

    @Test
    void postsMetadataAsJson() {
        ProcessorHttpMocks.SONG_SERVICE.stubFor(post(urlEqualTo("/songs")).willReturn(aResponse().withStatus(200)));

        songServiceClient.sendMetadata(new SongMetadataDto(3L, "Yesterday", "The Beatles", "Help!", "02:05", "1965"));

        ProcessorHttpMocks.SONG_SERVICE.verify(postRequestedFor(urlEqualTo("/songs"))
                .withRequestBody(equalToJson("""
                        {"id": 3, "name": "Yesterday", "artist": "The Beatles", "album": "Help!", "duration": "02:05", "year": "1965"}
                        """)));
    }

    @Test
    void metadataPostIsRetriedWhenSongServiceKeepsFailing() {
        ProcessorHttpMocks.SONG_SERVICE.stubFor(post(urlEqualTo("/songs")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> songServiceClient.sendMetadata(new SongMetadataDto(4L, "n", "a", "b", "01:00", "2000")))
                .isInstanceOf(HttpServerErrorException.class);

        ProcessorHttpMocks.SONG_SERVICE.verify(3, postRequestedFor(urlEqualTo("/songs")));
    }
}
