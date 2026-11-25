package com.course.microservices.resourceprocessor.tracing;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTracingConfig {

    @Bean
    static BeanPostProcessor rabbitTracingBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RabbitTemplate template) {
                    template.addBeforePublishPostProcessors(message -> {
                        String traceId = TraceId.current();
                        if (traceId != null) {
                            message.getMessageProperties().setHeader(TraceId.HEADER, traceId);
                        }
                        return message;
                    });
                }
                if (bean instanceof AbstractRabbitListenerContainerFactory<?> factory) {
                    factory.setAdviceChain(traceIdAdvice());
                }
                return bean;
            }
        };
    }

    private static Advice traceIdAdvice() {
        return (MethodInterceptor) invocation -> {
            String traceId = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Message message) {
                    Object header = message.getMessageProperties().getHeader(TraceId.HEADER);
                    traceId = header == null ? null : header.toString();
                    break;
                }
            }
            if (!TraceId.isValid(traceId)) {
                traceId = TraceId.generate();
            }
            TraceId.set(traceId);
            try {
                return invocation.proceed();
            } finally {
                TraceId.clear();
            }
        };
    }
}
