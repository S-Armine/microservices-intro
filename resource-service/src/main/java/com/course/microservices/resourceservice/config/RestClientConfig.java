package com.course.microservices.resourceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient songServiceRestClient(@Value("${song.service.url}") String baseUrl,
                                 @Value("${song.service.path}") String path) {
        return RestClient.builder()
                .baseUrl(baseUrl + path)
                .build();
    }
}
