package com.course.microservices.resourceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.course.microservices.resourceservice.tracing.TraceIdRestClientInterceptor;
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
                .requestInterceptor(new TraceIdRestClientInterceptor())
                .build();
    }

    @Bean
    public RestClient storageServiceRestClient(@Value("${storage.service.url}") String baseUrl,
                                               @Value("${storage.service.path}") String path,
                                               LoadBalancerClient loadBalancerClient) {
        return RestClient.builder()
                .baseUrl(baseUrl + path)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .requestInterceptor(new TraceIdRestClientInterceptor())
                .build();
    }
}
