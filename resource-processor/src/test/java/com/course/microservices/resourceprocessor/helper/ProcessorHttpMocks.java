package com.course.microservices.resourceprocessor.helper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class ProcessorHttpMocks {

    public static final WireMockServer RESOURCE_SERVICE = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    public static final WireMockServer SONG_SERVICE = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        RESOURCE_SERVICE.start();
        SONG_SERVICE.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RESOURCE_SERVICE.stop();
            SONG_SERVICE.stop();
        }));
    }

    private ProcessorHttpMocks() {
    }

    public static void reset() {
        RESOURCE_SERVICE.resetAll();
        SONG_SERVICE.resetAll();
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.resource-service[0].uri", RESOURCE_SERVICE::baseUrl);
        registry.add("spring.cloud.discovery.client.simple.instances.song-service[0].uri", SONG_SERVICE::baseUrl);
    }
}
