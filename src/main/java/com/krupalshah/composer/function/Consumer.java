package com.krupalshah.composer.function;

@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
}