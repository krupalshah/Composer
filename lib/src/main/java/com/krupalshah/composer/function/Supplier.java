package com.krupalshah.composer.function;

@FunctionalInterface
public interface Supplier<T> {
    T get() throws Exception;
}