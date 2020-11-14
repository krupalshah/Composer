package com.krupalshah.composer.function.collector;

@FunctionalInterface
public interface Collector<T, R> {
    R collect(T result);
}