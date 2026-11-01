package com.course.microservices.resourceservice.contract;

import com.course.microservices.resourceservice.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.Message;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierObjectMapper;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(MessagingContractBase.Config.class)
public abstract class MessagingContractBase {

    @MockitoBean
    private com.course.microservices.resourceservice.service.ResourceStorageService storageService;

    @org.springframework.beans.factory.annotation.Autowired
    private ResourceService resourceService;

    @org.springframework.beans.factory.annotation.Autowired
    private InMemoryAmqp.CapturingRabbitTemplate rabbitTemplate;

    @BeforeEach
    void clearCapturedMessages() {
        rabbitTemplate.clear();
    }

    public void resourceUploaded() {
        resourceService.uploadResource(new byte[]{1, 2, 3});
    }

    @TestConfiguration
    static class Config {
        @Bean
        InMemoryAmqp.CapturingRabbitTemplate rabbitTemplate() {
            return new InMemoryAmqp.CapturingRabbitTemplate();
        }

        @Bean
        InMemoryAmqp.Sender messageVerifierSender() {
            return new InMemoryAmqp.Sender();
        }

        @Bean
        InMemoryAmqp.Receiver messageVerifierReceiver(InMemoryAmqp.CapturingRabbitTemplate rabbitTemplate) {
            return new InMemoryAmqp.Receiver(rabbitTemplate);
        }

        @Bean
        ContractVerifierMessaging<Message> contractVerifierMessaging(
                InMemoryAmqp.Sender sender,
                InMemoryAmqp.Receiver receiver) {
            return new InMemoryAmqp.Messaging(sender, receiver);
        }

        @Bean
        ContractVerifierObjectMapper contractVerifierObjectMapper() {
            return new ContractVerifierObjectMapper();
        }
    }
}
