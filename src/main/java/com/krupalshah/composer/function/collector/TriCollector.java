package com.krupalshah.composer.function.collector;

@FunctionalInterface
public interface TriCollector<S, T, U, R> {
    R collect(S result1, T result2, U result3);
}
