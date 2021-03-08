package com.krupalshah.composer;


import com.krupalshah.composer.exception.ComposerException;
import com.krupalshah.composer.exception.ErrorStream;
import com.krupalshah.composer.function.collector.BiCollector;
import com.krupalshah.composer.function.collector.Collector;
import com.krupalshah.composer.function.collector.Distributor;
import com.krupalshah.composer.function.collector.TriCollector;
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
    Composable<T> thenExecute(SimpleTask task);

    /**
     * <p>Executes set of asynchronous runnable tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param tasks @return chained composable
     */
    Composable<T> thenExecuteTogether(SimpleTask... tasks);

    /**
     * <p>Synchronously executes a runnable task</p>
     *
     * @param task task which takes no input and returns no output
     * @return chained composable
     */
    Composable<T> thenWaitFor(SimpleTask task);

    /**
     * <p>Executes an asynchronous consumer task<p>
     *
     * @param task task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenExecute(ConsumingTask<T> task);

    /**
     * <p>Executes set of asynchronous consumer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     *
     * @param tasks supplier of tasks which takes an input but returns no output
     * @return chained composable
     */
    @SuppressWarnings("unchecked")
    Composable<T> thenExecuteTogether(ConsumingTask<T>... tasks);

    /**
     * <p>Expands the upstream value to a collection, executes an asynchronous consumer task for each value in that collection concurrently and waits for all to complete</p>
     *
     * @param distributor function which takes upstream result as an input and returns a collection from it
     * @param task        task to be run for each value in the collection returned from expander
     * @param <S>         type of value to be consumed by task
     * @return chained composable
     */
    <S> Composable<T> thenExecuteForEach(Distributor<T, Collection<S>> distributor, ConsumingTask<S> task);

    /**
     * <p>Synchronously executes a consumer task</p>
     *
     * @param task task which takes an input but returns no output
     * @return chained composable
     */
    Composable<T> thenWaitFor(ConsumingTask<T> task);

    /**
     * <p>Executes an asynchronous producer task<p>
     *
     * @param task task which takes no input but returns an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenExecute(ProducingTask<R> task);

    /**
     * <p>Executes collection of asynchronous producer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     * <p>The order of results received to the collector is only guaranteed if the collection implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param <S>              type of task output
     * @param <R>              type of collector output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param tasks            task which takes no input but produces an output of type S
     * @return chained composable
     */
    @SuppressWarnings("unchecked")
    <S, R> Composable<R> thenExecuteTogether(Collector<T, Set<S>, R> resultsCollector, ProducingTask<S>... tasks);

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
    <S, U, R> Composable<R> thenExecuteTogether(ProducingTask<S> task1, ProducingTask<U> task2, BiCollector<T, S, U, R> resultsCollector);

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
    <S, U, V, R> Composable<R> thenExecuteTogether(ProducingTask<S> task1, ProducingTask<U> task2, ProducingTask<V> task3, TriCollector<T, S, U, V, R> resultsCollector);

    /**
     * <p>Synchronously executes a producer task</p>
     *
     * @param task task which takes no input but returns an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenWaitFor(ProducingTask<R> task);

    /**
     * <p>Executes an asynchronous transformer task<p>
     *
     * @param task task which takes an input and transforms it into an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenExecute(TransformingTask<T, R> task);

    /**
     * <p>Executes collection of asynchronous transformer tasks concurrently and waits for all to complete.</p>
     * <p>The order of execution is never guaranteed.</p>
     * <p>The order of results received to the collector is only guaranteed if the collection implementation for tasks provided is an order one, such as {@link java.util.LinkedHashSet}</p>
     *
     * @param <S>              type of task output
     * @param <R>              type of collector output
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param tasks            task which takes an input and produce an output of type S
     * @return chained composable
     */
    @SuppressWarnings("unchecked")
    <S, R> Composable<R> thenExecuteTogether(Collector<T, Set<S>, R> resultsCollector, TransformingTask<T, S>... tasks);

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
    <S, U, R> Composable<R> thenExecuteTogether(TransformingTask<T, S> task1, TransformingTask<T, U> task2, BiCollector<T, S, U, R> resultsCollector);

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
    <S, U, V, R> Composable<R> thenExecuteTogether(TransformingTask<T, S> task1, TransformingTask<T, U> task2, TransformingTask<T, V> task3, TriCollector<T, S, U, V, R> resultsCollector);

    /**
     * <p>Expands the upstream value to a collection, executes an asynchronous transformer task for each value in that collection concurrently and waits for all to complete</p>
     *
     * @param distributor      function which takes upstream result as an input and returns a collection from it
     * @param task             task to be run for each value in the collection returned from expander
     * @param resultsCollector function which takes results received from tasks as an input and collects them into a data structure/pojo
     * @param <S>              type of value to be transformed by task
     * @param <U>              type of task output
     * @param <R>              type of collector output
     * @return chained composable
     */
    <S, U, R> Composable<R> thenExecuteForEach(Distributor<T, Collection<S>> distributor, TransformingTask<S, U> task, Collector<T, Set<Pair<S, U>>, R> resultsCollector);

    /**
     * <p>Synchronously executes a transformer task</p>
     *
     * @param task task which takes an input and transforms it into an output
     * @param <R>  type of task output
     * @return chained composable
     */
    <R> Composable<R> thenWaitFor(TransformingTask<T, R> task);

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
    void thenFinish(ConsumingTask<T> upstreamResultConsumer);
}
