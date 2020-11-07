package com.krupalshah.composer;


import com.krupalshah.composer.function.BiFunction;
import com.krupalshah.composer.function.Function;
import com.krupalshah.composer.function.Predicate;
import com.krupalshah.composer.function.TriFunction;

import java.util.concurrent.Callable;


public interface Composable<T> {

    <R> Composable<R> thenCall(Callable<R> task);

    <S, U, R> Composable<R> thenCallTogether(Callable<S> firstTask, Callable<U> secondTask, BiFunction<S, U, R> resultCombiner);

    <S, U, V, R> Composable<R> thenCallTogether(Callable<S> firstTask, Callable<U> secondTask, Callable<V> thirdTask, TriFunction<S, U, V, R> resultCombiner);

    <R> Composable<R> thenProcess(Function<T, R> processor);

    Composable<T> thenRun(Runnable task);

    Composable<T> thenCheck(Predicate<T> validator);

    T listen();
}
