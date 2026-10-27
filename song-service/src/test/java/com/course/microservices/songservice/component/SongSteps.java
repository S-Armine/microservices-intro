package com.course.microservices.songservice.component;

import com.course.microservices.songservice.repo.SongRepository;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.course.microservices.songservice.helper.SongTestHelper.songJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SongSteps {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SongRepository songRepository;

    private ResultActions response;

    @Before
    public void clearSongs() {
        songRepository.deleteAll();
    }

    @Given("no song metadata is stored")
    public void noSongMetadataIsStored() {
        assertThat(songRepository.count()).isZero();
    }

    @Given("the metadata of song {long} is already stored")
    public void theMetadataOfSongIsAlreadyStored(long id) throws Exception {
        mockMvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(songJson(id)))
                .andExpect(status().isOk());
    }

    @When("the client submits the metadata of song {long}")
    public void theClientSubmitsTheMetadataOfSong(long id) throws Exception {
        response = mockMvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(songJson(id)));
    }

    @When("the client submits the metadata of song {long} with the duration {string}")
    public void theClientSubmitsTheMetadataWithDuration(long id, String duration) throws Exception {
        String body = songJson(id).replace("02:05", duration);
        response = mockMvc.perform(post("/songs").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @When("the client requests the metadata of song {long}")
    public void theClientRequestsTheMetadataOfSong(long id) throws Exception {
        response = mockMvc.perform(get("/songs/" + id));
    }

    @When("the client requests the metadata of song {string}")
    public void theClientRequestsTheMetadataOfSong(String id) throws Exception {
        response = mockMvc.perform(get("/songs/" + id));
    }

    @When("the client deletes the songs {string}")
    public void theClientDeletesTheSongs(String ids) throws Exception {
        response = mockMvc.perform(delete("/songs").param("id", ids));
    }

    @When("the client deletes a list of {int} song ids")
    public void theClientDeletesAListOfSongIds(int count) throws Exception {
        String ids = IntStream.rangeClosed(1, count).mapToObj(String::valueOf).collect(Collectors.joining(","));
        response = mockMvc.perform(delete("/songs").param("id", ids));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) throws Exception {
        response.andExpect(status().is(status));
    }

    @Then("the response contains the id {long}")
    public void theResponseContainsTheId(long id) throws Exception {
        response.andExpect(jsonPath("$.id").value(id));
    }

    @Then("the song is {string} by {string} from the album {string}")
    public void theSongIs(String name, String artist, String album) throws Exception {
        response.andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.artist").value(artist))
                .andExpect(jsonPath("$.album").value(album));
    }

    @Then("the song lasts {string} and was released in {string}")
    public void theSongLastsAndWasReleasedIn(String duration, String year) throws Exception {
        response.andExpect(jsonPath("$.duration").value(duration))
                .andExpect(jsonPath("$.year").value(year));
    }

    @Then("the error message is {string}")
    public void theErrorMessageIs(String message) throws Exception {
        response.andExpect(jsonPath("$.errorMessage").value(message));
    }

    @Then("the error message starts with {string}")
    public void theErrorMessageStartsWith(String prefix) throws Exception {
        response.andExpect(jsonPath("$.errorMessage").value(startsWith(prefix)));
    }

    @Then("the field {string} is reported as {string}")
    public void theFieldIsReportedAs(String field, String message) throws Exception {
        response.andExpect(jsonPath("$.details." + field).value(message));
    }

    @Then("the deleted ids are {string}")
    public void theDeletedIdsAre(String ids) throws Exception {
        String[] expected = ids.split(",");
        response.andExpect(jsonPath("$.ids.length()").value(expected.length));
        for (int i = 0; i < expected.length; i++) {
            response.andExpect(jsonPath("$.ids[" + i + "]").value(Long.parseLong(expected[i])));
        }
    }
}
