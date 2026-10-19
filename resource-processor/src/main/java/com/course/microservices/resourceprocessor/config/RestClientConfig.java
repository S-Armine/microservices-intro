package com.course.microservices.resourceprocessor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient songServiceRestClient(@Value("${song.service.url}") String baseUrl,
                                            @Value("${song.service.path}") String path,
                                            LoadBalancerClient loadBalancerClient) {
        return RestClient.builder()
                .baseUrl(baseUrl + path)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();
    }

    @Bean
    public RestClient resourceServiceRestClient(@Value("${resource.service.url}") String baseUrl,
                                               @Value("${resource.service.path}") String path,
                                               LoadBalancerClient loadBalancerClient) {
        return RestClient.builder()
                .baseUrl(baseUrl + path)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();
    }


}
