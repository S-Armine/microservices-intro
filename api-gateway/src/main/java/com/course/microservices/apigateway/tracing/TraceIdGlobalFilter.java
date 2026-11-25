package com.course.microservices.apigateway.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (traceId == null || !VALID.matcher(traceId).matches()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String id = traceId;

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, id)))
                .build();
        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });

        String method = mutated.getRequest().getMethod().name();
        String path = mutated.getRequest().getURI().getPath();
        return chain.filter(mutated).doFinally(signal -> logWithTraceId(id,
                method + " " + path + " -> " + mutated.getResponse().getStatusCode()));
    }

    private static void logWithTraceId(String traceId, String message) {
        MDC.put(MDC_KEY, traceId);
        try {
            log.info("{}", message);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
