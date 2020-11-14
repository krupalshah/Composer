package com.krupalshah.composer;

import com.krupalshah.composer.exception.ComposerException;
import com.krupalshah.composer.exception.ErrorStream;
import com.krupalshah.composer.function.collector.BiCollector;
import com.krupalshah.composer.function.collector.Collector;
import com.krupalshah.composer.function.collector.TriCollector;
import com.krupalshah.composer.function.tasks.ConsumingTask;
import com.krupalshah.composer.function.tasks.ProducingTask;
import com.krupalshah.composer.function.tasks.Task;
import com.krupalshah.composer.function.tasks.TransformingTask;
import com.krupalshah.composer.function.validator.Validator;
import com.krupalshah.composer.util.KnownFuture;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Implementation of {@link Composable} and serves as an entry point for all composer functions.
 * Use factory methods such as {@link #startWith(ProducingTask, ErrorStream)} to create new instance.
 */
public class Composer<T> implements Composable<T> {

    private final Future<T> future;
    private final ErrorStream errStream;
    private final ExecutorService executorService;

    private Composer(Future<T> future, ErrorStream errStream, ExecutorService executorService) {
        this.future = future;
        this.errStream = errStream;
        this.executorService = executorService;
    }

    //region factory methods

    /**
     * <p>Convenient factory method to create new composer instance.</p>
     * <p>Creates default {@link ExecutorService} using cached thread pool internally.</p>
     *
     * @param task      task which returns a result to begin with.
     * @param errStream consumer for all errors.
     * @param <R>       return type of task.
     * @return new composer instance.
     */
    public static <R> Composer<R> startWith(ProducingTask<R> task, ErrorStream errStream) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        return startWith(task, errStream, executorService);
    }

    /**
     * <p>Factory method to create new composer instance with provided executor service.</p>
     *
     * @param task            task which returns a result to begin with.
     * @param errStream       consume for all errors.
     * @param executorService executor service with custom thread pool to submit the task.
     * @param <R>             return type of task.
     * @return new composer instance.
     * @see #startWith(ProducingTask, ErrorStream)
     */
    public static <R> Composer<R> startWith(ProducingTask<R> task, ErrorStream errStream, ExecutorService executorService) {
        try {
            Future<R> future = executorService.submit(task::produce);
            return newComposer(future, errStream, executorService);
        } catch (Throwable t) {
            errStream.onError(t);
            return newComposer(null, errStream, executorService);
        }
    }
    //endregion

    //region public API
    @Override
    public Composable<T> thenRun(Task task) {
        return chainWith(() -> {
            T upstream = await();
            Future<T> resultFuture = async(() -> uncheckedTask(task), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenConsume(ConsumingTask<T> task) {
        return chainWith(() -> {
            T upstream = await();
            Future<T> resultFuture = async(() -> uncheckedTask(task, upstream), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public <R> Composable<R> thenProduce(ProducingTask<R> task) {
        return chainWith(() -> {
            await();
            Future<R> future = async(task::produce);
            return switchTo(future);
        });
    }

    @Override
    public <R> Composable<R> thenTransform(TransformingTask<? super T, ? extends R> task) {
        return chainWith(() -> {
            T upstream = await();
            Future<R> future = async(() -> task.transform(upstream));
            return switchTo(future);
        });
    }

    @Override
    public Composable<T> thenRunTogether(Set<Task> tasks) {
        return chainWith(() -> {
            await();
            CountDownLatch latch = newLatch(tasks.size());
            for (Task task : tasks) {
                async(() -> countdownTask(() -> uncheckedTask(task), latch));
            }
            latch.await();
            return this;
        });
    }

    @Override
    public Composable<T> thenConsumeTogether(Set<ConsumingTask<T>> tasks) {
        return chainWith(() -> {
            T upstream = await();
            CountDownLatch latch = newLatch(tasks.size());
            for (ConsumingTask<T> task : tasks) {
                async(() -> countdownTask(() -> uncheckedTask(task, upstream), latch));
            }
            latch.await();
            return this;
        });
    }

    @Override
    public <S, R> Composable<R> thenProduceTogether(Set<ProducingTask<? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector) {
        return chainWith(() -> {
            await();

            CountDownLatch latch = newLatch(tasks.size());
            Set<Future<? extends S>> futures = new LinkedHashSet<>();
            for (ProducingTask<? extends S> task : tasks) {
                Future<? extends S> future = async(() -> countdownTask(task::produce, latch));
                futures.add(future);
            }
            latch.await();

            Set<S> results = new LinkedHashSet<>();
            for (Future<? extends S> future : futures) {
                S result = future.get();
                results.add(result);
            }
            R result = resultsCollector.collect(results);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <S, U, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector) {
        return chainWith(() -> {
            await();

            CountDownLatch latch = newLatch(2);
            Future<? extends S> future1 = async(() -> countdownTask(task1::produce, latch));
            Future<? extends U> future2 = async(() -> countdownTask(task2::produce, latch));
            latch.await();

            S result1 = future1.get();
            U result2 = future2.get();
            R result = resultsCollector.collect(result1, result2);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenProduceTogether(ProducingTask<? extends S> task1, ProducingTask<? extends U> task2, ProducingTask<? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector) {
        return chainWith(() -> {
            await();

            CountDownLatch latch = newLatch(3);
            Future<? extends S> future1 = async(() -> countdownTask(task1::produce, latch));
            Future<? extends U> future2 = async(() -> countdownTask(task2::produce, latch));
            Future<? extends V> future3 = async(() -> countdownTask(task3::produce, latch));
            latch.await();

            S result1 = future1.get();
            U result2 = future2.get();
            V result3 = future3.get();
            R result = resultsCollector.collect(result1, result2, result3);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <S, R> Composable<R> thenTransformTogether(Set<TransformingTask<? super T, ? extends S>> tasks, Collector<Set<? super S>, ? extends R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();

            CountDownLatch latch = newLatch(tasks.size());
            Set<Future<? extends S>> futures = new LinkedHashSet<>();
            for (TransformingTask<? super T, ? extends S> task : tasks) {
                Future<? extends S> future = async(() -> countdownTask(() -> task.transform(upstream), latch));
                futures.add(future);
            }
            latch.await();

            Set<S> results = new LinkedHashSet<>();
            for (Future<? extends S> future : futures) {
                S result = future.get();
                results.add(result);
            }
            R result = resultsCollector.collect(results);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <S, U, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, BiCollector<? super S, ? super U, ? extends R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();

            CountDownLatch latch = newLatch(2);
            Future<? extends S> future1 = async(() -> countdownTask(() -> task1.transform(upstream), latch));
            Future<? extends U> future2 = async(() -> countdownTask(() -> task2.transform(upstream), latch));
            latch.await();

            S result1 = future1.get();
            U result2 = future2.get();
            R result = resultsCollector.collect(result1, result2);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenTransformTogether(TransformingTask<? super T, ? extends S> task1, TransformingTask<? super T, ? extends U> task2, TransformingTask<? super T, ? extends V> task3, TriCollector<? super S, ? super U, ? super V, ? extends R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();

            CountDownLatch latch = newLatch(3);
            Future<? extends S> future1 = async(() -> countdownTask(() -> task1.transform(upstream), latch));
            Future<? extends U> future2 = async(() -> countdownTask(() -> task2.transform(upstream), latch));
            Future<? extends V> future3 = async(() -> countdownTask(() -> task3.transform(upstream), latch));
            latch.await();

            S result1 = future1.get();
            U result2 = future2.get();
            V result3 = future3.get();
            R result = resultsCollector.collect(result1, result2, result3);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public Composable<T> thenRunSynchronously(Task task) {
        return chainWith(() -> {
            await();
            task.run();
            return this;
        });
    }

    @Override
    public Composable<T> thenConsumeSynchronously(ConsumingTask<T> task) {
        return chainWith(() -> {
            T upstream = await();
            task.consume(upstream);
            return this;
        });
    }

    @Override
    public <R> Composable<R> thenProduceSynchronously(ProducingTask<R> task) {
        return chainWith(() -> {
            await();
            R result = task.produce();
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <R> Composable<R> thenTransformSynchronously(TransformingTask<? super T, ? extends R> task) {
        return chainWith(() -> {
            T upstream = await();
            R result = task.transform(upstream);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public Composable<T> thenCheckIf(Validator<? super T> validator) {
        return chainWith(() -> {
            T upstream = await();
            if (validator.validate(upstream)) {
                return this;
            } else {
                errStream.onError(new ComposerException(String.format("The upstream result %s " +
                        "is not valid as per the validator provided! Downstream execution will stop now.", upstream)));
                return switchTo(null);
            }
        });
    }

    @Override
    public T finish() {
        try {
            return await();
        } catch (Throwable t) {
            errStream.onError(t);
            return null;
        }
    }
    //endregion

    //region private API
    private static <R> Composer<R> newComposer(Future<R> future, ErrorStream errStream, ExecutorService executorService) {
        return new Composer<>(future, errStream, executorService);
    }

    private <R> Composer<R> switchTo(Future<R> resultFuture) {
        return newComposer(resultFuture, this.errStream, this.executorService);
    }

    private <R> Composer<R> chainWith(Callable<Composer<R>> composerSupplier) {
        if (this.future == null) {
            return switchTo(null);
        } else {
            try {
                return composerSupplier.call();
            } catch (Throwable t) {
                errStream.onError(t);
                return switchTo(null);
            }
        }
    }

    private <R> Future<R> async(Callable<R> task) {
        return executorService.submit(task);
    }

    private <R> Future<R> async(Runnable task, R result) {
        return executorService.submit(task, result);
    }

    private void async(Runnable task) {
        executorService.submit(task);
    }

    private T await() throws InterruptedException, ExecutionException {
        if (this.future == null) {
            return null;
        }
        return this.future.get();
    }

    private CountDownLatch newLatch(int nTasks) {
        return new CountDownLatch(nTasks);
    }

    private <R> R countdownTask(Callable<R> task, CountDownLatch latch) throws Exception {
        try {
            return task.call();
        } finally {
            latch.countDown();
        }
    }

    private void countdownTask(Runnable task, CountDownLatch latch) {
        try {
            task.run();
        } finally {
            latch.countDown();
        }
    }

    private void uncheckedTask(Task task) {
        try {
            task.run();
        } catch (Exception e) {
            throw new ComposerException(e);
        }
    }

    private <S> void uncheckedTask(ConsumingTask<S> task, S input) {
        try {
            task.consume(input);
        } catch (Exception e) {
            throw new ComposerException(e);
        }
    }
    //endregion

}
