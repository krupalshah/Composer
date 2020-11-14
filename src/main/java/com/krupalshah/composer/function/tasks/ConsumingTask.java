package com.krupalshah.composer.function.tasks;

@FunctionalInterface
public interface ConsumingTask<T> {
    void consume(T input) throws Exception;
}