package com.course.microservices.resourceprocessor.component;

import com.course.microservices.resourceprocessor.helper.ProcessorHttpMocks;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest
public class CucumberSpringConfiguration {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        ProcessorHttpMocks.registerProperties(registry);
    }
}
