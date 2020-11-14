package com.krupalshah.composer;


import com.krupalshah.composer.function.*;

import java.util.Set;
import java.util.concurrent.Callable;


public interface Composable<T> {

    <R> Composable<R> thenCall(Callable<R> task);

    <S, R> Composable<R> thenCall(Set<Callable<? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner);

    <S, U, R> Composable<R> thenCall(Callable<? extends S> firstTask, Callable<? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner);

    <S, U, V, R> Composable<R> thenCall(Callable<? extends S> firstTask, Callable<? extends U> secondTask, Callable<? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner);

    <R> Composable<R> thenCallSynchronously(Callable<R> task);

    <R> Composable<R> thenTransform(Function<? super T, ? extends R> task);

    <S, R> Composable<R> thenTransform(Set<Function<? super T, ? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner);

    <S, U, R> Composable<R> thenTransform(Function<? super T, ? extends S> firstTask, Function<? super T, ? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner);

    <S, U, V, R> Composable<R> thenTransform(Function<? super T, ? extends S> firstTask, Function<? super T, ? extends U> secondTask, Function<? super T, ? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner);

    <R> Composable<R> thenTransformSynchronously(Function<? super T, ? extends R> task);

    Composable<T> thenConsume(Consumer<T> task);

    Composable<T> thenConsume(Set<Consumer<T>> tasks);

    Composable<T> thenConsumeSynchronously(Consumer<T> task);

    Composable<T> thenExecute(Runnable task);

    Composable<T> thenExecute(Set<Runnable> tasks);

    Composable<T> thenExecuteSynchronously(Runnable task);

    Composable<T> thenCheckIf(Predicate<? super T> validator);

    T finish();
}
