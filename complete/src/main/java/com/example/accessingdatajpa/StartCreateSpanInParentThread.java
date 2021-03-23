package com.example.accessingdatajpa;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;

public class StartCreateSpanInParentThread {

    public static void main(String[] args) throws InterruptedException {
        Tracer openTracer = io.opentracing.util.GlobalTracer.get();
        ScopeManager scopeManager = openTracer.scopeManager();
        System.out.println("active span: " + openTracer.activeSpan());

        SpanBuilder builder = openTracer.buildSpan("root");
        Span rootSpan = builder.ignoreActiveSpan().start();
        Scope scope = scopeManager.activate(rootSpan, true);

        System.out.println("active span: " + openTracer.activeSpan());

        SpanBuilder builder2 = openTracer.buildSpan("span-to-be-closed-in-another-thread");
        Span span = builder2./*asChildOf(rootSpan).*/start();
        System.out.println("active span: " + openTracer.activeSpan());
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("active span thread: " + openTracer.activeSpan());
            span.finish();
        });

        thread.start();
        Thread.sleep(800);

        Span span2 = openTracer.buildSpan("beyond-root-span-after-scope-is-closed").start();
        thread.join();

        Thread.sleep(1200);

        Span activeSpan = openTracer.activeSpan();
        System.out.println("activeSpan: " + activeSpan);
        rootSpan.finish();

        Runnable scopeCloseRunnable = () -> scope.close();

        if (true) {
            scopeCloseRunnable.run();
        } else {
            Thread thread1 = new Thread(scopeCloseRunnable);
            thread1.start();
            thread1.join();
        }

        activeSpan = openTracer.activeSpan();
        System.out.println("activeSpan: " + activeSpan);

        Thread.sleep(1000);
        span2.finish();
        Thread.sleep(5000);
    }
}
