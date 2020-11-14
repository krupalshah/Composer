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


public interface Composable<T> {

    Composable<T> thenRun(Task task);

    Composable<T> thenConsume(ConsumingTask<T> task);

    <R> Composable<R> thenProduce(ProducingTask<R> task);

    <R> Composable<R> thenTransform(TransformingTask<? super T, ? extends R> task);

    Composable<T> thenRunTogether(Set<Task> tasks);

    Composable<T> thenConsumeTogether(Set<ConsumingTask<T>> tasks);

    <S, R> Composable<R> thenProduceTogether(Set<ProducingTask<? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector);

    <S, U, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector);

    <S, U, V, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, ProducingTask<? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector);

    <S, R> Composable<R> thenTransformTogether(Set<TransformingTask<? super T, ? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector);

    <S, U, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector);

    <S, U, V, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, TransformingTask<? super T, ? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector);

    Composable<T> thenRunSynchronously(Task task);

    Composable<T> thenConsumeSynchronously(ConsumingTask<T> task);

    <R> Composable<R> thenProduceSynchronously(ProducingTask<R> task);

    <R> Composable<R> thenTransformSynchronously(TransformingTask<? super T, ? extends R> task);

    Composable<T> thenCheckIf(Validator<? super T> validator);

    T finish();
}
