package com.course.microservices.resourceservice.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TraceId.HEADER);
        if (!TraceId.isValid(traceId)) {
            traceId = TraceId.generate();
        }
        TraceId.set(traceId);
        response.setHeader(TraceId.HEADER, traceId);
        boolean actuator = request.getRequestURI().startsWith("/actuator");
        try {
            chain.doFilter(request, response);
        } finally {
            if (!actuator) {
                log.info("{} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
            }
            TraceId.clear();
        }
    }
}
