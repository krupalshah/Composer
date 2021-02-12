package com.krupalshah.composer;


import com.krupalshah.composer.exception.ComposerException;
import com.krupalshah.composer.exception.ErrorStream;
import com.krupalshah.composer.function.collector.BiCollector;
import com.krupalshah.composer.function.collector.Collector;
import com.krupalshah.composer.function.collector.Expander;
import com.krupalshah.composer.function.collector.TriCollector;
import com.krupalshah.composer.function.other.Consumer;
import com.krupalshah.composer.function.other.Supplier;
import com.krupalshah.composer.function.other.Validator;
import com.krupalshah.composer.function.tasks.ConsumingTask;
import com.krupalshah.composer.function.tasks.ProducingTask;
import com.krupalshah.composer.function.tasks.SimpleTask;
import com.krupalshah.composer.function.tasks.TransformingTask;
import com.krupalshah.composer.util.Pair;

import java.util.Collection;
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
     * <p>Executes set of asynchronous runnable tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param tasksSupplier supplier of tasks which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenRunTogether(Supplier<Collection<SimpleTask>> tasksSupplier);

    /**
     * <p>Synchronously executes a runnable task</p>
     *
     * @param task task which takes no input and returns no output
     * @return chained composable
     */
    Composable<T> thenRunSynchronously(SimpleTask task);

    /**
     * <p>Executes an asynchronous consumer task<p>
     *
     * @param task task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenConsume(ConsumingTask<T> task);

    /**
     * <p>Executes set of asynchronous consumer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param tasksSupplier supplier of tasks which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenConsumeTogether(Supplier<Collection<ConsumingTask<T>>> tasksSupplier);

    /**
     * <p>Transforms the upstream value to a collection, executes an asynchronous consumer task for each value in that collection concurrently and waits for all to complete</p>
     *
     * @param expander function which takes upstream result as an input and returns a collection from it
     * @param task     task to be run for each value in the collection returned from expander
     * @param <S>      type of value to be consumed by task
     * @return chained composable
     */
    <S> Composable<T> thenConsumeForEachTogether(Expander<T, Collection<S>> expander, ConsumingTask<S> task);

    /**
     * <p>Synchronously executes a consumer task</p>
     *
     * @param task task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenConsumeSynchronously(ConsumingTask<T> task);

    /**
     * <p>Executes an asynchronous producer task<p>
     *
     * @param task task which takes no input but returns an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenProduce(ProducingTask<R> task);

    /**
     * <p>Executes collection of asynchronous producer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     * <p>The order of results received to the collector is only guaranteed if the collection implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param <S>              type of task output
     * @param <R>              type of collector output
     * @param tasksSupplier    supplier of tasks which takes no input but returns an output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @return chained composable
     */
    <S, R> Composable<R> thenProduceTogether(Supplier<Collection<ProducingTask<S>>> tasksSupplier, Collector<T, Set<S>, R> resultsCollector);

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
    <S, U, R> Composable<R> thenProduceTogether(ProducingTask<S> task1, ProducingTask<U> task2, BiCollector<T, S, U, R> resultsCollector);

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
    <S, U, V, R> Composable<R> thenProduceTogether(ProducingTask<S> task1, ProducingTask<U> task2, ProducingTask<V> task3, TriCollector<T, S, U, V, R> resultsCollector);

    /**
     * <p>Synchronously executes a producer task</p>
     *
     * @param task task which takes no input but returns an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenProduceSynchronously(ProducingTask<R> task);

    /**
     * <p>Executes an asynchronous transformer task<p>
     *
     * @param task task which takes an input and transforms it into an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenTransform(TransformingTask<T, R> task);

    /**
     * <p>Executes collection of asynchronous transformer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     * <p>The order of results received to the collector is only guaranteed if the collection implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param <S>              type of task output
     * @param <R>              type of collector output
     * @param tasksSupplier    supplier of tasks which takes an input and transforms it into an output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @return chained composable
     */
    <S, R> Composable<R> thenTransformTogether(Supplier<Collection<TransformingTask<T, S>>> tasksSupplier, Collector<T, Set<S>, R> resultsCollector);

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
    <S, U, R> Composable<R> thenTransformTogether(TransformingTask<T, S> task1, TransformingTask<T, U> task2, BiCollector<T, S, U, R> resultsCollector);

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
    <S, U, V, R> Composable<R> thenTransformTogether(TransformingTask<T, S> task1, TransformingTask<T, U> task2, TransformingTask<T, V> task3, TriCollector<T, S, U, V, R> resultsCollector);

    /**
     * <p>Transforms the upstream value to a collection, executes an asynchronous transformer task for each value in that collection concurrently and waits for all to complete</p>
     *
     * @param expander         function which takes upstream result as an input and returns a collection from it
     * @param task             task to be run for each value in the collection returned from expander
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of value to be transformed by task
     * @param <U>              type of task output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, U, R> Composable<R> thenTransformForEachTogether(Expander<T, Collection<S>> expander, TransformingTask<S, U> task, Collector<T, Set<Pair<S, U>>, R> resultsCollector);

    /**
     * <p>Synchronously executes a transformer task</p>
     *
     * @param task task which takes an input and transforms it into an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenTransformSynchronously(TransformingTask<T, R> task);

    /**
     * <p>Applies specified condition and continues only if it returns true.</p>
     * <p>If condition returns false, you will receive {@link ComposerException} in the provided {@link ErrorStream}</p>
     *
     * @param validator function which provides condition to check
     * @return chained composable
     */
    Composable<T> thenContinueIf(Validator<T> validator);

    /**
     * <p>discontinues chaining of tasks and completes the awaiting task execution</p>
     */
    void thenFinish();

    /**
     * <p>discontinues chaining of tasks, completes the awaiting task execution and supplies the last producing task result</p>
     *
     * @param upstreamResultConsumer consumer receiving an output of the last producing task in the discontinued chain
     */
    void thenFinish(Consumer<T> upstreamResultConsumer);
}
