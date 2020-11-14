package com.krupalshah.composer.function.tasks;

@FunctionalInterface
public interface ProducingTask<R> {
    R produce() throws Exception;
}
