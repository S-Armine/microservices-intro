package com.course.microservices.resourceprocessor.contract;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class DeliveringAmqp {

    private DeliveringAmqp() {
    }

    static final class Sender implements MessageVerifierSender<Message> {

        private final RabbitListenerEndpointRegistry registry;
        private final SimpleMessageConverter converter = new SimpleMessageConverter();
        private final JsonMapper jsonMapper = JsonMapper.builder().build();

        Sender(RabbitListenerEndpointRegistry registry) {
            this.registry = registry;
        }

        @Override
        public void send(Message message, String destination, YamlContract contract) {
            if (!"resource-processing-exchange".equals(destination)
                    || !"resource-processing-routing-key".equals(
                            message.getMessageProperties().getReceivedRoutingKey())) {
                throw new IllegalArgumentException("Unexpected resource upload destination or routing key");
            }
            var containers = registry.getListenerContainers().stream()
                    .filter(AbstractMessageListenerContainer.class::isInstance)
                    .map(AbstractMessageListenerContainer.class::cast)
                    .filter(container -> Arrays.asList(container.getQueueNames()).contains("resource-processing"))
                    .toList();
            if (containers.size() != 1
                    || !(containers.getFirst().getMessageListener() instanceof ChannelAwareMessageListener listener)) {
                throw new IllegalStateException("Expected one AMQP listener for resource-processing");
            }
            try {
                listener.onMessage(message, null);
            } catch (Exception exception) {
                // The AMQP listener interface declares checked exceptions.
                throw new IllegalStateException("Resource upload listener failed", exception);
            }
        }

        @Override
        public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
            MessageProperties properties = new MessageProperties();
            properties.setReceivedExchange(destination);
            Object routingKey = headers == null ? null : headers.get("amqp_receivedRoutingKey");
            if (!(routingKey instanceof String key)) {
                throw new IllegalArgumentException("Missing resource upload routing key");
            }
            properties.setReceivedRoutingKey(key);
            if (!(payload instanceof String serialized)) {
                throw new IllegalArgumentException("Expected a JSON-encoded Stub Runner payload");
            }
            // StubRunnerExecutor JSON-encodes DSL bodies, including plain string messages.
            Object body = jsonMapper.readValue(serialized, Object.class);
            if (!(body instanceof String text)) {
                throw new IllegalArgumentException("Resource upload body must be a string");
            }
            send(converter.toMessage(text, properties), destination, contract);
        }
    }

    static final class Receiver implements MessageVerifierReceiver<Message> {

        @Override
        public Message receive(String destination, long timeout, TimeUnit unit, YamlContract contract) {
            throw new UnsupportedOperationException("resource-processor only consumes messages");
        }

        @Override
        public Message receive(String destination, YamlContract contract) {
            throw new UnsupportedOperationException("resource-processor only consumes messages");
        }
    }

    static final class Messaging extends ContractVerifierMessaging<Message> {

        Messaging(Sender sender, Receiver receiver) {
            super(sender, receiver);
        }

        @Override
        protected ContractVerifierMessage convert(Message message) {
            if (message == null) {
                return null;
            }
            return new ContractVerifierMessage(new String(message.getBody(), StandardCharsets.UTF_8),
                    message.getMessageProperties().getHeaders());
        }
    }
}
