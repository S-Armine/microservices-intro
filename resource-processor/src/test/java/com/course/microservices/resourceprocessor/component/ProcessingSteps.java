package com.course.microservices.resourceprocessor.component;

import com.course.microservices.resourceprocessor.consumer.ResourceConsumer;
import com.course.microservices.resourceprocessor.helper.Mp3Fixtures;
import com.course.microservices.resourceprocessor.helper.ProcessorHttpMocks;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

public class ProcessingSteps {

    @Autowired
    private ResourceConsumer consumer;

    private Throwable failure;

    @Before
    public void resetServices() {
        ProcessorHttpMocks.reset();
        failure = null;
    }

    @Given("the resource service and the song service are reachable")
    public void bothServicesAreReachable() {
    }

    @Given("the resource service holds the song {string} by {string} from the album {string} released in {string} as resource {long}")
    public void theResourceServiceHoldsTheSong(String title, String artist, String album, String year, long id) {
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/" + id))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "audio/mpeg")
                        .withBody(Mp3Fixtures.mp3(title, artist, album, year, 96))));
    }

    @Given("the resource service holds a file that is not an MP3 as resource {long}")
    public void theResourceServiceHoldsANonMp3(long id) {
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/" + id))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "audio/mpeg")
                        .withBody("just some text".getBytes(StandardCharsets.UTF_8))));
    }

    @Given("the resource service does not know resource {long}")
    public void theResourceServiceDoesNotKnowResource(long id) {
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo("/resources/" + id))
                .willReturn(aResponse().withStatus(404)));
    }

    @Given("the resource service fails once and then holds the song {string} by {string} from the album {string} released in {string} as resource {long}")
    public void theResourceServiceFailsOnce(String title, String artist, String album, String year, long id) {
        String url = "/resources/" + id;
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo(url)).inScenario("outage")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503)).willSetStateTo("back"));
        ProcessorHttpMocks.RESOURCE_SERVICE.stubFor(get(urlEqualTo(url)).inScenario("outage")
                .whenScenarioStateIs("back")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "audio/mpeg")
                        .withBody(Mp3Fixtures.mp3(title, artist, album, year, 96))));
    }

    @Given("the song service accepts new metadata")
    public void theSongServiceAcceptsNewMetadata() {
        ProcessorHttpMocks.SONG_SERVICE.stubFor(post(urlEqualTo("/songs"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{\"id\":1}")));
    }

    @Given("the song service already has metadata for song {long}")
    public void theSongServiceAlreadyHasMetadata(long id) {
        ProcessorHttpMocks.SONG_SERVICE.stubFor(post(urlEqualTo("/songs"))
                .willReturn(aResponse().withStatus(409).withHeader("Content-Type", "application/json")
                        .withBody("{\"errorMessage\":\"Metadata for resource ID=" + id + " already exists\",\"errorCode\":\"409\"}")));
    }

    @When("the id {string} arrives on the processing queue")
    public void theIdArrives(String id) {
        try {
            consumer.receiveMessage(id);
        } catch (RuntimeException ex) {
            failure = ex;
        }
    }

    @Then("the song service receives metadata for song {long}")
    public void theSongServiceReceivesMetadata(long id) {
        assertThat(failure).isNull();
        assertThat(JsonPath.<Number>read(lastSongRequestBody(), "$.id").longValue()).isEqualTo(id);
    }

    @Then("the metadata names the song {string} by {string} from the album {string}")
    public void theMetadataNamesTheSong(String title, String artist, String album) {
        String body = lastSongRequestBody();
        assertThat(JsonPath.<String>read(body, "$.name")).isEqualTo(title);
        assertThat(JsonPath.<String>read(body, "$.artist")).isEqualTo(artist);
        assertThat(JsonPath.<String>read(body, "$.album")).isEqualTo(album);
    }

    @Then("the metadata has the year {string} and the duration {string}")
    public void theMetadataHasYearAndDuration(String year, String duration) {
        String body = lastSongRequestBody();
        assertThat(JsonPath.<String>read(body, "$.year")).isEqualTo(year);
        assertThat(JsonPath.<String>read(body, "$.duration")).isEqualTo(duration);
    }

    @Then("the song service receives nothing")
    public void theSongServiceReceivesNothing() {
        ProcessorHttpMocks.SONG_SERVICE.verify(0, postRequestedFor(urlEqualTo("/songs")));
    }

    @Then("processing fails with the message {string}")
    public void processingFailsWithTheMessage(String message) {
        assertThat(failure).hasMessage(message);
    }

    @Then("processing fails because the resource was not found")
    public void processingFailsBecauseNotFound() {
        assertThat(failure).isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Then("processing fails because the song already exists")
    public void processingFailsBecauseTheSongExists() {
        assertThat(failure).isInstanceOf(HttpClientErrorException.Conflict.class);
    }

    private String lastSongRequestBody() {
        List<LoggedRequest> requests = ProcessorHttpMocks.SONG_SERVICE.findAll(postRequestedFor(urlEqualTo("/songs")));
        assertThat(requests).as("requests received by song-service").hasSize(1);
        return requests.get(0).getBodyAsString();
    }
}
