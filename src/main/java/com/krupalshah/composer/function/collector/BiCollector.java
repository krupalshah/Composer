package com.krupalshah.composer.function.collector;

@FunctionalInterface
public interface BiCollector<T, U, R> {
    R collect(T result1, U result2);
}