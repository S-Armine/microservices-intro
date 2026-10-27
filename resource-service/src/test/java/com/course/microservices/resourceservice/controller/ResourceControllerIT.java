package com.course.microservices.resourceservice.controller;

import com.course.microservices.resourceservice.entity.Resource;
import com.course.microservices.resourceservice.repo.ResourceRepository;
import com.course.microservices.resourceservice.helper.SongServiceMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ResourceRepository repository;

    @MockitoBean
    private S3Client s3Client;
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void songService(DynamicPropertyRegistry registry) {
        SongServiceMock.registerProperties(registry);
    }

    @BeforeEach
    void reset() {
        repository.deleteAll();
        SongServiceMock.SERVER.resetAll();
    }

    @Test
    void uploadStoresTheFileSavesTheRowAndPublishesTheId() throws Exception {
        mockMvc.perform(post("/resources").contentType("audio/mpeg").content(new byte[]{1, 2, 3}))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        Resource saved = repository.findAll().get(0);
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(rabbitTemplate).convertAndSend("resource-processing-exchange", "resource-processing-routing-key",
                saved.getId().toString());
    }

    @Test
    void uploadOfSomethingThatIsNotMp3IsRejected() throws Exception {
        mockMvc.perform(post("/resources").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage")
                        .value("Invalid file format: application/json. Only MP3 files are allowed"));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void failedS3UploadIsAnInternalErrorAndNothingIsPublished() throws Exception {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("s3 down"));

        mockMvc.perform(post("/resources").contentType("audio/mpeg").content(new byte[]{1}))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage").value("Failed to upload resource to S3"));

        assertThat(repository.count()).isZero();
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void downloadReturnsTheStoredAudio() throws Exception {
        Resource resource = repository.save(new Resource(null, "resources", "resources/x.mp3", LocalDateTime.now()));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[]{4, 5}));

        mockMvc.perform(get("/resources/" + resource.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(new byte[]{4, 5}));
    }

    @Test
    void deleteTellsSongServiceThenRemovesFileAndRow() throws Exception {
        Resource resource = repository.save(new Resource(null, "resources", "resources/y.mp3", LocalDateTime.now()));
        SongServiceMock.SERVER.stubFor(com.github.tomakehurst.wiremock.client.WireMock.delete(urlPathEqualTo("/songs"))
                .withQueryParam("id", equalTo(resource.getId().toString()))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"ids\":[" + resource.getId() + "]}")));

        mockMvc.perform(delete("/resources").param("id", resource.getId() + ",999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids.length()").value(1))
                .andExpect(jsonPath("$.ids[0]").value(resource.getId()));

        SongServiceMock.SERVER.verify(deleteRequestedFor(urlPathEqualTo("/songs")));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        assertThat(repository.count()).isZero();
    }
}
