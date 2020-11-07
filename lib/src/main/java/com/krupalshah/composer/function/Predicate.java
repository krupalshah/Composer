package com.krupalshah.composer.function;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
}