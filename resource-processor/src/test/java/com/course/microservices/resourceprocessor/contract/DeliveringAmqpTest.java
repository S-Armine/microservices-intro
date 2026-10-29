package com.course.microservices.resourceprocessor.contract;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class DeliveringAmqpTest {

    private final RabbitListenerEndpointRegistry registry = mock(RabbitListenerEndpointRegistry.class);
    private final DeliveringAmqp.Sender sender = new DeliveringAmqp.Sender(registry);

    @Test
    void rejectsAnUnexpectedExchangeBeforeDelivery() {
        assertThatThrownBy(() -> sender.send(message("resource-processing-routing-key"), "wrong-exchange", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destination or routing key");
        verifyNoInteractions(registry);
    }

    @Test
    void rejectsAnUnexpectedRoutingKeyBeforeDelivery() {
        assertThatThrownBy(() -> sender.send(message("wrong-key"), "resource-processing-exchange", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destination or routing key");
        verifyNoInteractions(registry);
    }

    @Test
    void rejectsMissingRoutingHeadersBeforeDelivery() {
        assertThatThrownBy(() -> sender.send("\"1\"", null, "resource-processing-exchange", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routing key");
        verifyNoInteractions(registry);
    }

    @Test
    void rejectsANumericBodyInsteadOfSilentlyConvertingItToAString() {
        assertThatThrownBy(() -> sender.send("1",
                Map.of("amqp_receivedRoutingKey", "resource-processing-routing-key"),
                "resource-processing-exchange", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Resource upload body must be a string");
        verifyNoInteractions(registry);
    }

    private Message message(String routingKey) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
        properties.setReceivedRoutingKey(routingKey);
        return new Message("1".getBytes(StandardCharsets.UTF_8), properties);
    }
}
