package com.krupalshah.composer.function.other;

@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}