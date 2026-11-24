package com.course.microservices.resourceservice.helper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;

public final class StorageServiceMock {

    public static final WireMockServer SERVER = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        SERVER.start();
        Runtime.getRuntime().addShutdownHook(new Thread(SERVER::stop));
    }

    private StorageServiceMock() {
    }

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.storage-service[0].uri", SERVER::baseUrl);
    }
}
