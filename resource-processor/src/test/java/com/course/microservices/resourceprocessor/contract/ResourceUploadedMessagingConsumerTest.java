package com.course.microservices.resourceprocessor.contract;

import com.course.microservices.resourceprocessor.service.ResourceProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.stubrunner.StubTrigger;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureStubRunner(
        ids = "com.course.microservices:resource-service:0.0.1-SNAPSHOT:stubs",
        stubsMode = StubRunnerProperties.StubsMode.REMOTE)
@Import(ResourceUploadedMessagingConsumerTest.Config.class)
class ResourceUploadedMessagingConsumerTest {

    @Autowired
    private StubTrigger stubTrigger;

    @MockitoBean
    private ResourceProcessingService resourceProcessingService;

    @Test
    void triggeringTheResourceUploadedStubHandsTheIdToTheProcessingService() {
        assertThat(stubTrigger.trigger("resource_uploaded")).isTrue();

        verify(resourceProcessingService, timeout(5000)).processResource("1");
    }

    @TestConfiguration
    static class Config {

        @Bean
        DeliveringAmqp.Sender messageVerifierSender(RabbitListenerEndpointRegistry registry) {
            return new DeliveringAmqp.Sender(registry);
        }

        @Bean
        DeliveringAmqp.Receiver messageVerifierReceiver() {
            return new DeliveringAmqp.Receiver();
        }

        @Bean
        ContractVerifierMessaging<Message> contractVerifierMessaging(
                DeliveringAmqp.Sender sender,
                DeliveringAmqp.Receiver receiver) {
            return new DeliveringAmqp.Messaging(sender, receiver);
        }
    }
}
