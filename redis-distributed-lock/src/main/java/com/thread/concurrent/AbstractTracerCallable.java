package com.thread.concurrent;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

public abstract class AbstractTracerCallable<V> implements Callable<V> {
    private Map<String, String> context;

    public AbstractTracerCallable() {
        this.context = MDC.getCopyOfContextMap();
    }
    @Override
    public V call() throws Exception {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        if (context == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
        try {
            return callWithMdc();
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }


    public abstract V callWithMdc();
}
