package com.example.accessingdatajpa;

import io.opentracing.Span;

public class CurrentSpanThreadLocal {
    private static ThreadLocal<Span> currentSpan =  new ThreadLocal<>();

    public static Span getCurrentSpan() {
        return currentSpan.get();
    }

    public static void setCurrentSpan(Span span) {
        currentSpan.set(span);
    }
}
