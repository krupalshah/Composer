package com.krupalshah.composer;


import com.krupalshah.composer.function.*;

import java.util.Set;
import java.util.concurrent.Callable;


public interface Composable<T> {

    <R> Composable<R> thenCall(Callable<R> task);

    <S, R> Composable<R> thenCallTogether(Set<Callable<? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner);

    <S, U, R> Composable<R> thenCallTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner);

    <S, U, V, R> Composable<R> thenCallTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, Callable<? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner);

    Composable<T> thenCallSynchronously(Callable<T> task);

    Composable<T> thenExecute(Runnable task);

    Composable<T> thenExecuteTogether(Runnable... tasks);

    Composable<T> thenExecuteSynchronously(Runnable task);

    Composable<T> thenConsume(Consumer<T> task);

    Composable<T> thenConsumeTogether(Consumer<T>... tasks);

    Composable<T> thenConsumeSynchronously(Consumer<T> task);

    Composable<T> thenCheck(Predicate<? super T> validator);

    <R> Composable<R> thenProcess(Function<? super T, ? extends R> task);

    <S, R> Composable<R> thenProcessTogether(Set<Function<? super T, ? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner);

    <S, U, R> Composable<R> thenProcessTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner);

    <S, U, V, R> Composable<R> thenProcessTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, Callable<? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner);

    <R> Composable<R> thenProcessSynchronously(Function<? super T, ? extends R> task);

    T listen();
}
