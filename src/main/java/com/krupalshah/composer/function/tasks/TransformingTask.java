package com.krupalshah.composer.function.tasks;

@FunctionalInterface
public interface TransformingTask<T, R> {
    R transform(T input) throws Exception;
}
