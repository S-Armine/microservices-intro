package com.course.microservices.resourceservice.tracing;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class TraceId {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private TraceId() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isValid(String value) {
        return value != null && VALID.matcher(value).matches();
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static void set(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    public static <T> T callWith(String traceId, Supplier<T> supplier) {
        if (traceId == null) {
            return supplier.get();
        }
        String previous = current();
        set(traceId);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                clear();
            } else {
                set(previous);
            }
        }
    }
}
