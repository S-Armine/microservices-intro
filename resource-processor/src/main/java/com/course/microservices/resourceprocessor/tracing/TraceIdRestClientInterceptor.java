package com.course.microservices.resourceprocessor.tracing;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class TraceIdRestClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = TraceId.current();
        if (traceId != null) {
            request.getHeaders().set(TraceId.HEADER, traceId);
        }
        return execution.execute(request, body);
    }
}
