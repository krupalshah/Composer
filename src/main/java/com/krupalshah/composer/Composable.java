package com.krupalshah.composer;


import com.krupalshah.composer.function.collector.BiCollector;
import com.krupalshah.composer.function.collector.Collector;
import com.krupalshah.composer.function.collector.TriCollector;
import com.krupalshah.composer.function.tasks.ConsumingTask;
import com.krupalshah.composer.function.tasks.ProducingTask;
import com.krupalshah.composer.function.tasks.Task;
import com.krupalshah.composer.function.tasks.TransformingTask;
import com.krupalshah.composer.function.validator.Validator;

import java.util.Set;

/**
 * <P>Interface for composing chain of asynchronous tasks</P>
 *
 * @param <T> type of upstream result
 */
public interface Composable<T> {

    /**
     * <P>Executes an asynchronous runnable task<P>
     *
     * @param task task which takes no input and returns no output
     */
    Composable<T> thenRun(Task task);

    /**
     * <P>Executes an asynchronous consumer task<P>
     *
     * @param task task which takes an input but returns no output
     */
    Composable<T> thenConsume(ConsumingTask<T> task);

    /**
     * <P>Executes an asynchronous producer task<P>
     *
     * @param task task which takes no input but returns an output
     */
    <R> Composable<R> thenProduce(ProducingTask<R> task);

    /**
     * <P>Executes an asynchronous transformer task<P>
     *
     * @param task task which takes an input and transforms it into an output
     */
    <R> Composable<R> thenTransform(TransformingTask<? super T, ? extends R> task);

    /**
     * <P>Executes set of asynchronous runnable tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     *
     * @param tasks task which takes an input but returns no output
     */
    Composable<T> thenRunTogether(Set<Task> tasks);

    /**
     * <P>Executes set of asynchronous consumer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     *
     * @param tasks task which takes an input but returns no output
     */
    Composable<T> thenConsumeTogether(Set<ConsumingTask<T>> tasks);

    /**
     * <P>Executes set of asynchronous producer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     * <p>The order of results received to the collector is only guaranteed if the set implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param tasks            task which takes no input but returns an output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task output
     * @param <R>              type of collector output
     */
    <S, R> Composable<R> thenProduceTogether(Set<ProducingTask<? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector);

    /**
     * <P>Executes asynchronous producer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     *
     * @param task1            task which takes no input but returns an output of type S
     * @param task2            task which takes no input but returns an output of type U
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <R>              type of collector output
     */
    <S, U, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector);

    /**
     * <P>Executes asynchronous producer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     *
     * @param task1            task which takes no input but returns an output of type S
     * @param task2            task which takes no input but returns an output of type U
     * @param task3            task which takes no input but returns an output of type V
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <V>              type of task2 output
     * @param <R>              type of collector output
     */
    <S, U, V, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, ProducingTask<? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector);

    /**
     * <P>Executes set of asynchronous transformer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     * <p>The order of results received to the collector is only guaranteed if the set implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param tasks            task which takes an input and transforms it into an output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task output
     * @param <R>              type of collector output
     */
    <S, R> Composable<R> thenTransformTogether(Set<TransformingTask<? super T, ? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector);

    /**
     * <P>Executes asynchronous transformer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     *
     * @param task1            task which takes an input and transforms it into an output of type S
     * @param task2            task which takes an input and transforms it into an output of type U
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <R>              type of collector output
     */
    <S, U, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector);

    /**
     * <P>Executes asynchronous transformer tasks concurrently and waits for all to complete.</P>
     * <P>The order of execution is never guaranteed.</P>
     *
     * @param task1            task which takes an input and transforms it into an output of type S
     * @param task2            task which takes an input and transforms it into an output of type U
     * @param task3            task which takes an input and transforms it into an output of type V
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <V>              type of task3 output
     * @param <R>              type of collector output
     */
    <S, U, V, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, TransformingTask<? super T, ? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector);

    Composable<T> thenRunSynchronously(Task task);

    Composable<T> thenConsumeSynchronously(ConsumingTask<T> task);

    <R> Composable<R> thenProduceSynchronously(ProducingTask<R> task);

    <R> Composable<R> thenTransformSynchronously(TransformingTask<? super T, ? extends R> task);

    Composable<T> thenCheckIf(Validator<? super T> validator);

    T finish();
}
