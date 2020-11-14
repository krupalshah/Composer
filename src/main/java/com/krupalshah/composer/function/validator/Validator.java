package com.krupalshah.composer.function.validator;

@FunctionalInterface
public interface Validator<T> {
    boolean validate(T input);
}