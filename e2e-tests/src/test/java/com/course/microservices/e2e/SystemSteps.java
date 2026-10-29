package com.course.microservices.e2e;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assumptions;

import java.net.URI;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemSteps {

    private static final String RESOURCE_SERVICE_URL = env("E2E_RESOURCE_SERVICE_URL", "http://localhost:8080");
    private static final String SONG_SERVICE_URL = env("E2E_SONG_SERVICE_URL", "http://localhost:8081");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    private byte[] mp3;
    private HttpResponse<byte[]> response;
    private HttpResponse<byte[]> downloaded;
    private long resourceId;

    @Before
    public void skipWhenTheSystemIsNotRunning() throws InterruptedException {
        Assumptions.assumeTrue(isUp(RESOURCE_SERVICE_URL + "/resources/1") && isUp(SONG_SERVICE_URL + "/songs/1"),
                "The system is not running (docker compose up), skipping end-to-end scenarios");
    }

    @Given("an MP3 file of the song {string} by {string} from the album {string} released in {string}")
    public void anMp3File(String title, String artist, String album, String year) {
        mp3 = Mp3Fixtures.mp3(title, artist, album, year);
    }

    @When("the file is uploaded to the resource service")
    public void theFileIsUploaded() throws Exception {
        response = send(HttpRequest.newBuilder(URI.create(RESOURCE_SERVICE_URL + "/resources"))
                .header("Content-Type", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofByteArray(mp3)).build());
        assertThat(response.statusCode()).as("MP3 upload status").isEqualTo(200);
        resourceId = JsonPath.parse(new String(response.body(), StandardCharsets.UTF_8)).read("$.id", Long.class);
        assertThat(resourceId).isPositive();
    }

    @When("a file of type {string} is uploaded to the resource service")
    public void aFileOfTypeIsUploaded(String type) throws Exception {
        response = send(HttpRequest.newBuilder(URI.create(RESOURCE_SERVICE_URL + "/resources"))
                .header("Content-Type", type)
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build());
    }

    @When("the file is downloaded from the resource service")
    public void theFileIsDownloaded() throws Exception {
        downloaded = send(HttpRequest.newBuilder(URI.create(RESOURCE_SERVICE_URL + "/resources/" + resourceId)).GET().build());
    }

    @When("the resource with id {long} is downloaded")
    public void theResourceWithIdIsDownloaded(long id) throws Exception {
        response = send(HttpRequest.newBuilder(URI.create(RESOURCE_SERVICE_URL + "/resources/" + id)).GET().build());
    }

    @When("the resource is deleted")
    public void theResourceIsDeleted() throws Exception {
        response = send(HttpRequest.newBuilder(URI.create(RESOURCE_SERVICE_URL + "/resources?id=" + resourceId))
                .DELETE().build());
    }

    @Then("the resource service answers with the id of the new resource")
    public void theResourceServiceAnswersWithTheId() {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(resourceId).isPositive();
    }

    @Then("within {int} seconds the song service has metadata for that id")
    public void withinSecondsTheSongServiceHasMetadata(int seconds) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(seconds).toNanos();
        HttpResponse<byte[]> song;
        do {
            song = send(HttpRequest.newBuilder(URI.create(SONG_SERVICE_URL + "/songs/" + resourceId)).GET().build());
            if (song.statusCode() == 200) {
                response = song;
                return;
            }
            Thread.sleep(1000);
        } while (System.nanoTime() < deadline);
        assertThat(song.statusCode()).as("song metadata for resource %d after %d seconds", resourceId, seconds).isEqualTo(200);
    }

    @Then("the metadata names the song {string} by {string} from the album {string}")
    public void theMetadataNamesTheSong(String title, String artist, String album) {
        String json = new String(response.body());
        assertThat(JsonPath.<String>read(json, "$.name")).isEqualTo(title);
        assertThat(JsonPath.<String>read(json, "$.artist")).isEqualTo(artist);
        assertThat(JsonPath.<String>read(json, "$.album")).isEqualTo(album);
    }

    @Then("the metadata has the year {string} and the duration {string}")
    public void theMetadataHasYearAndDuration(String year, String duration) {
        String json = new String(response.body());
        assertThat(JsonPath.<String>read(json, "$.year")).isEqualTo(year);
        assertThat(JsonPath.<String>read(json, "$.duration")).isEqualTo(duration);
    }

    @Then("the downloaded content equals the uploaded file")
    public void theDownloadedContentEqualsTheUploadedFile() {
        assertThat(downloaded.statusCode()).isEqualTo(200);
        assertThat(downloaded.body()).isEqualTo(mp3);
    }

    @Then("the resource service lists the id as deleted")
    public void theResourceServiceListsTheIdAsDeleted() {
        assertThat(response.statusCode()).isEqualTo(200);
        java.util.List<Number> ids = JsonPath.read(new String(response.body(), StandardCharsets.UTF_8), "$.ids");
        assertThat(ids.stream().map(Number::longValue).toList()).containsExactly(resourceId);
    }

    @Then("the resource service no longer finds the resource")
    public void theResourceServiceNoLongerFindsTheResource() throws Exception {
        HttpResponse<byte[]> again = send(HttpRequest.newBuilder(URI.create(RESOURCE_SERVICE_URL + "/resources/" + resourceId)).GET().build());
        assertThat(again.statusCode()).isEqualTo(404);
    }

    @Then("the song service no longer has metadata for that id")
    public void theSongServiceNoLongerHasMetadata() throws Exception {
        HttpResponse<byte[]> song = send(HttpRequest.newBuilder(URI.create(SONG_SERVICE_URL + "/songs/" + resourceId)).GET().build());
        assertThat(song.statusCode()).isEqualTo(404);
    }

    @Then("the resource service refuses it with status {int}")
    public void theResourceServiceRefusesItWithStatus(int status) {
        assertThat(response.statusCode()).isEqualTo(status);
    }

    @Then("the resource service answers with status {int}")
    public void theResourceServiceAnswersWithStatus(int status) {
        assertThat(response.statusCode()).isEqualTo(status);
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws Exception {
        HttpRequest boundedRequest = HttpRequest.newBuilder(request, (name, value) -> true)
                .timeout(Duration.ofSeconds(3)).build();
        return http.send(boundedRequest, HttpResponse.BodyHandlers.ofByteArray());
    }

    private boolean isUp(String url) throws InterruptedException {
        try {
            int status = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
            assertThat(status).as("readiness response from %s", url).isIn(200, 404);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
