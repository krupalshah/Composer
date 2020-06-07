package com.krupalshah.composer.function;

@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T firstInput, U secondInput) throws Exception;
}