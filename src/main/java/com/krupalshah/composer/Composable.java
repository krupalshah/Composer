package com.krupalshah.composer;


import com.krupalshah.composer.function.*;

import java.util.Set;
import java.util.concurrent.Callable;


public interface Composable<T> {

    <R> Composable<R> thenCall(Callable<R> task);

    <S, R> Composable<R> thenCallTogether(Set<Callable<? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner);

    <S, U, R> Composable<R> thenCallTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner);

    <S, U, V, R> Composable<R> thenCallTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, Callable<? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner);

    <R> Composable<R> thenCallBlocking(Callable<R> task);

    <R> Composable<R> thenTransform(Function<? super T, ? extends R> task);

    <S, R> Composable<R> thenTransformTogether(Set<Function<? super T, ? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner);

    <S, U, R> Composable<R> thenTransformTogether(Function<? super T, ? extends S> firstTask, Function<? super T, ? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner);

    <S, U, V, R> Composable<R> thenTransformTogether(Function<? super T, ? extends S> firstTask, Function<? super T, ? extends U> secondTask, Function<? super T, ? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner);

    <R> Composable<R> thenTransformBlocking(Function<? super T, ? extends R> task);

    Composable<T> thenConsume(Consumer<T> task);

    Composable<T> thenConsumeTogether(Set<Consumer<T>> tasks);

    Composable<T> thenConsumeBlocking(Consumer<T> task);

    Composable<T> thenRun(Runnable task);

    Composable<T> thenRunTogether(Set<Runnable> tasks);

    Composable<T> thenRunBlocking(Runnable task);

    Composable<T> thenCheckIf(Predicate<? super T> validator);

    T finish();
}
