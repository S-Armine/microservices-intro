package com.course.microservices.resourceprocessor.contract;

import com.course.microservices.resourceprocessor.service.ResourceServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.course.microservices:resource-service:0.0.1-SNAPSHOT:stubs",
        stubsMode = StubRunnerProperties.StubsMode.REMOTE)
class ResourceServiceStubConsumerTest {

    @Autowired
    private ResourceServiceClient resourceServiceClient;

    @Test
    void fetchesTheResourceBytesDefinedByTheGetResourceContract() {
        assertThat(resourceServiceClient.fetchResourceData("1"))
                .isEqualTo("ID3-fake-mp3-bytes".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void propagatesNotFoundAsDefinedByTheGetResourceNotFoundContract() {
        assertThatThrownBy(() -> resourceServiceClient.fetchResourceData("404"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
