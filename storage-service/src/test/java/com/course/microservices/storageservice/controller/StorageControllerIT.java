package com.course.microservices.storageservice.controller;

import com.course.microservices.storageservice.repo.StorageRepository;
import com.course.microservices.storageservice.service.BucketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StorageControllerIT {

    private static final String BODY = """
            {"storageType": "PERMANENT", "bucket": "bucket_name", "path": "/files"}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StorageRepository repository;

    @MockitoBean
    private BucketService bucketService;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void createReturnsIdAndProvisionsTheBucket() throws Exception {
        mockMvc.perform(post("/storages").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());

        verify(bucketService).ensureBucketExists("bucket_name");
        assertThat(repository.findAll()).singleElement().satisfies(s -> assertThat(s.getPath()).isEqualTo("files"));
    }

    @Test
    void createRejectsDuplicateBucketPath() throws Exception {
        mockMvc.perform(post("/storages").contentType(MediaType.APPLICATION_JSON).content(BODY));

        mockMvc.perform(post("/storages").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createRejectsUnknownStorageType() throws Exception {
        mockMvc.perform(post("/storages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storageType\": \"OTHER\", \"bucket\": \"b\", \"path\": \"p\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsAllStorages() throws Exception {
        mockMvc.perform(post("/storages").contentType(MediaType.APPLICATION_JSON).content(BODY));

        mockMvc.perform(get("/storages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storageType").value("PERMANENT"))
                .andExpect(jsonPath("$[0].bucket").value("bucket_name"));
    }

    @Test
    void deleteIgnoresMissingIds() throws Exception {
        mockMvc.perform(post("/storages").contentType(MediaType.APPLICATION_JSON).content(BODY));
        Long id = repository.findAll().get(0).getId();

        mockMvc.perform(delete("/storages").param("id", id + ",9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]").value(id))
                .andExpect(jsonPath("$.ids.length()").value(1));

        assertThat(repository.count()).isZero();
    }

    @Test
    void deleteRejectsInvalidAndTooLongCsv() throws Exception {
        mockMvc.perform(delete("/storages").param("id", "abc")).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/storages").param("id", "1,".repeat(101) + "1")).andExpect(status().isBadRequest());
    }
}
