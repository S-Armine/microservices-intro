package com.course.microservices.resourceprocessor.component;

import com.course.microservices.resourceprocessor.helper.ProcessorHttpMocks;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        ProcessorHttpMocks.registerProperties(registry);
    }
}
