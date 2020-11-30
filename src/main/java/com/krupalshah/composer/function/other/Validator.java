package com.krupalshah.composer.function.other;

@FunctionalInterface
public interface Validator<T> {
    boolean validate(T input);
}