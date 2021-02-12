package com.krupalshah.composer.function.collector;

@FunctionalInterface
public interface TriCollector<S, T, U, V, R> {
    R collect(S upstream, T result1, U result2, V result3);
}
