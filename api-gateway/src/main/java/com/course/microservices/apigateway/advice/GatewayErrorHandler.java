package com.course.microservices.apigateway.advice;

import io.netty.channel.ConnectTimeoutException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

/**
 * Converts every gateway failure into a JSON body that does not leak internals.
 * Order -2 puts it ahead of Spring Boot's default error handler.
 */
@Component
@Order(-2)
public class GatewayErrorHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatusCode status = resolveStatus(ex);
        String path = exchange.getRequest().getPath().value();
        if (status.is5xxServerError()) {
            log.error("Gateway error for {}: {}", path, ex.toString());
        } else {
            log.debug("Gateway error for {}: {}", path, ex.toString());
        }

        String json = "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}"
                .formatted(Instant.now(), status.value(), reason(status), message(status), escape(path));

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }

    private HttpStatusCode resolveStatus(Throwable ex) {
        if (ex instanceof NotFoundException nf) {
            return nf.getStatusCode();
        }
        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode();
        }
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConnectTimeoutException || t instanceof ConnectException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (t instanceof TimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String reason(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }

    private String message(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "The requested resource or route does not exist.";
            case 405 -> "This HTTP method is not supported for the requested route.";
            case 502 -> "Received an invalid response from the upstream service.";
            case 503 -> "The service is temporarily unavailable. Please try again later.";
            case 504 -> "The service took too long to respond. Please try again later.";
            default -> status.is4xxClientError()
                    ? "The request could not be processed."
                    : "An unexpected error occurred. Please try again later.";
        };
    }

    private String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> sb.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return sb.toString();
    }
}
