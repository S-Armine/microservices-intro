package com.course.microservices.resourceservice.contract;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.contract.verifier.converter.YamlContract;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierReceiver;
import org.springframework.cloud.contract.verifier.messaging.MessageVerifierSender;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessage;
import org.springframework.cloud.contract.verifier.messaging.internal.ContractVerifierMessaging;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class InMemoryAmqp {

    private InMemoryAmqp() {
    }

    static final class CapturingRabbitTemplate extends RabbitTemplate {
        private final Map<String, BlockingQueue<Message>> sent = new ConcurrentHashMap<>();

        CapturingRabbitTemplate() {
            super(new CachingConnectionFactory("localhost"));
        }

        @Override
        public void convertAndSend(String exchange, String routingKey, Object payload) {
            MessageProperties properties = new MessageProperties();
            properties.setContentType("text/plain");
            properties.setReceivedExchange(exchange);
            properties.setReceivedRoutingKey(routingKey);
            byte[] body = String.valueOf(payload).getBytes(StandardCharsets.UTF_8);
            sent.computeIfAbsent(exchange, ignored -> new LinkedBlockingQueue<>())
                    .add(new Message(body, properties));
        }

        Message poll(String destination, long timeout, TimeUnit unit) {
            try {
                return sent.computeIfAbsent(destination, ignored -> new LinkedBlockingQueue<>())
                        .poll(timeout, unit);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        void clear() {
            sent.clear();
        }
    }

    static final class Receiver implements MessageVerifierReceiver<Message> {
        private final CapturingRabbitTemplate template;

        Receiver(CapturingRabbitTemplate template) {
            this.template = template;
        }

        @Override
        public Message receive(String destination, long timeout, TimeUnit unit, YamlContract contract) {
            return template.poll(destination, timeout, unit);
        }

        @Override
        public Message receive(String destination, YamlContract contract) {
            return receive(destination, 5, TimeUnit.SECONDS, contract);
        }
    }

    static final class Sender implements MessageVerifierSender<Message> {
        @Override
        public void send(Message message, String destination, YamlContract contract) {
            throw new UnsupportedOperationException("resource-service only produces messages");
        }

        @Override
        public <T> void send(T payload, Map<String, Object> headers, String destination, YamlContract contract) {
            throw new UnsupportedOperationException("resource-service only produces messages");
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
            Map<String, Object> headers = new HashMap<>();
            headers.put("amqp_receivedRoutingKey", message.getMessageProperties().getReceivedRoutingKey());
            headers.put("amqp_receivedExchange", message.getMessageProperties().getReceivedExchange());
            headers.put("contentType", message.getMessageProperties().getContentType());
            return new ContractVerifierMessage(new String(message.getBody(), StandardCharsets.UTF_8), headers);
        }
    }
}
