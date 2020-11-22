package com.krupalshah.composer;


import com.krupalshah.composer.exception.ComposerException;
import com.krupalshah.composer.exception.ErrorStream;
import com.krupalshah.composer.function.collector.BiCollector;
import com.krupalshah.composer.function.collector.Collector;
import com.krupalshah.composer.function.collector.TriCollector;
import com.krupalshah.composer.function.tasks.ConsumingTask;
import com.krupalshah.composer.function.tasks.ProducingTask;
import com.krupalshah.composer.function.tasks.SimpleTask;
import com.krupalshah.composer.function.tasks.TransformingTask;
import com.krupalshah.composer.function.validator.Validator;

import java.util.Set;

/**
 * <p>Interface for composing chain of asynchronous tasks</p>
 *
 * @param <T> type of upstream result
 */
public interface Composable<T> {

    /**
     * <p>Executes an asynchronous runnable task<p>
     *
     * @param task task which takes no input and returns no output
     * @return chained composable
     */
    Composable<T> thenRun(SimpleTask task);

    /**
     * <p>Executes an asynchronous consumer task<p>
     *
     * @param task task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenConsume(ConsumingTask<T> task);

    /**
     * <p>Executes an asynchronous producer task<p>
     *
     * @param task task which takes no input but returns an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenProduce(ProducingTask<R> task);

    /**
     * <p>Executes an asynchronous transformer task<p>
     *
     * @param task task which takes an input and transforms it into an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenTransform(TransformingTask<? super T, ? extends R> task);

    /**
     * <p>Executes set of asynchronous runnable tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param tasks task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenRunTogether(Set<SimpleTask> tasks);

    /**
     * <p>Executes set of asynchronous consumer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param tasks task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenConsumeTogether(Set<ConsumingTask<T>> tasks);

    /**
     * <p>Executes set of asynchronous producer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     * <p>The order of results received to the collector is only guaranteed if the set implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param tasks            task which takes no input but returns an output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, R> Composable<R> thenProduceTogether(Set<ProducingTask<? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector);

    /**
     * <p>Executes asynchronous producer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param task1            task which takes no input but returns an output of type S
     * @param task2            task which takes no input but returns an output of type U
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, U, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector);

    /**
     * <p>Executes asynchronous producer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param task1            task which takes no input but returns an output of type S
     * @param task2            task which takes no input but returns an output of type U
     * @param task3            task which takes no input but returns an output of type V
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <V>              type of task2 output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, U, V, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, ProducingTask<? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector);

    /**
     * <p>Executes set of asynchronous transformer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     * <p>The order of results received to the collector is only guaranteed if the set implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param tasks            task which takes an input and transforms it into an output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, R> Composable<R> thenTransformTogether(Set<TransformingTask<? super T, ? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector);

    /**
     * <p>Executes asynchronous transformer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param task1            task which takes an input and transforms it into an output of type S
     * @param task2            task which takes an input and transforms it into an output of type U
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, U, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector);

    /**
     * <p>Executes asynchronous transformer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param task1            task which takes an input and transforms it into an output of type S
     * @param task2            task which takes an input and transforms it into an output of type U
     * @param task3            task which takes an input and transforms it into an output of type V
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of task1 output
     * @param <U>              type of task2 output
     * @param <V>              type of task3 output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, U, V, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, TransformingTask<? super T, ? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector);

    /**
     * <p>Synchronously executes a runnable task</p>
     *
     * @param task task which takes no input and returns no output
     * @return chained composable
     */
    Composable<T> thenRunSynchronously(SimpleTask task);

    /**
     * <p>Synchronously executes a consumer task</p>
     *
     * @param task task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenConsumeSynchronously(ConsumingTask<T> task);

    /**
     * <p>Synchronously executes a producer task</p>
     *
     * @param task task which takes no input but returns an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenProduceSynchronously(ProducingTask<R> task);

    /**
     * <p>Synchronously executes a transformer task</p>
     *
     * @param task task which takes an input and transforms it into an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenTransformSynchronously(TransformingTask<? super T, ? extends R> task);

    /**
     * <p>Applies specified condition and continues only if it returns true.</p>
     * <p>If condition returns false, you will receive {@link ComposerException} in the provided {@link ErrorStream}</p>
     *
     * @param validator function which provides condition to check
     * @return chained composable
     */
    Composable<T> thenContinueIf(Validator<? super T> validator);

    /**
     * <p>Discontinues chaining of tasks and returns any awaiting upstream result</p>
     *
     * @return output of the last task in discontinued chain
     */
    T thenFinish();
}
