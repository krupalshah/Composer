package com.krupalshah.composer.function;

@FunctionalInterface
public interface Callable<V> {
    V call() throws Exception;
}
