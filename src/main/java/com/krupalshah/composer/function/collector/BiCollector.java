package com.krupalshah.composer.function.collector;

@FunctionalInterface
public interface BiCollector<S, T, U, R> {
    R collect(S upstream, T result1, U result2);
}