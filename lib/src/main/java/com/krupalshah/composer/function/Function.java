package com.krupalshah.composer.function;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T input) throws Exception;
}