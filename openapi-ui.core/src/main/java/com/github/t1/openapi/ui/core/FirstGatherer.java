package com.github.t1.openapi.ui.core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Gatherer;

/// A minimalistic Gatherer that applies an operation to the first element
public record FirstGatherer<T>(Consumer<T> operation) implements Gatherer<T, AtomicBoolean, T> {
    @Override public Supplier<AtomicBoolean> initializer() {return () -> new AtomicBoolean(true);}

    @Override public Integrator<AtomicBoolean, T, T> integrator() {
        return (state, element, downstream) -> {
            if (state.compareAndSet(true, false)) {
                operation.accept(element);
            }
            return downstream.push(element);
        };
    }
}
