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
import com.krupalshah.composer.util.KnownFuture;
import com.krupalshah.composer.util.Pair;

import java.util.*;
import java.util.concurrent.*;

/**
 * Implementation of {@link Composable} and serves as an entry point for all composer functions.
 * Use factory methods such as {@link #startWith} to create new instance.
 */
public class Composer<T> implements Composable<T> {

    private static final ExecutorService PARENT_EXECUTOR = Executors.newSingleThreadExecutor();
    private final ExecutorService taskExecutor;
    private final Future<T> future;
    private final ErrorStream errStream;

    private Composer(Future<T> future, ErrorStream errStream, ExecutorService taskExecutor) {
        this.future = future;
        this.errStream = errStream;
        this.taskExecutor = taskExecutor;
    }

    //region factory methods

    /**
     * <p>Convenient factory method to create new composer instance with no pre-known values.</p>
     * <p>Creates default {@link ExecutorService} using cached thread pool internally.</p>
     *
     * @param errStream consumer for all errors.
     * @return new composer instance.
     */
    public static Composable<?> startWith(ErrorStream errStream) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        return startWith(errStream, executorService);
    }

    /**
     * <p>Convenient factory method to create new composer instance with some pre-known value.</p>
     * <p>Creates default {@link ExecutorService} using cached thread pool internally.</p>
     *
     * @param value     pre-known value to begin with
     * @param errStream consumer for all errors.
     * @param <R>       type of task output.
     * @return new composer instance.
     */
    public static <R> Composable<R> startWith(R value, ErrorStream errStream) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        return startWith(value, errStream, executorService);
    }

    /**
     * <p>Convenient factory method to create new composer instance with asynchronous runnable task.</p>
     * <p>Creates default {@link ExecutorService} using cached thread pool internally.</p>
     *
     * @param task      task which takes no input and returns no output
     * @param errStream consumer for all errors.
     * @return new composer instance.
     */
    public static Composable<?> startWith(SimpleTask task, ErrorStream errStream) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        return startWith(task, errStream, executorService);
    }

    /**
     * <p>Convenient factory method to create new composer instance with asynchronous producer task.</p>
     * <p>Creates default {@link ExecutorService} using cached thread pool internally.</p>
     *
     * @param task      task which produces an output
     * @param errStream consumer for all errors.
     * @param <R>       type of task output.
     * @return new composer instance.
     */
    public static <R> Composable<R> startWith(ProducingTask<R> task, ErrorStream errStream) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        return startWith(task, errStream, executorService);
    }

    /**
     * <p>Factory method to create new composer instance with no pre-known values and custom executor service.</p>
     * <p>Use only in the cases you need to specify custom executor service to execute tasks.</p>
     *
     * @param errStream    consumer for all errors.
     * @param taskExecutor executor service with custom thread pool to submit the task.
     * @return new composer instance.
     * @see #startWith(Object, ErrorStream)
     */
    public static Composable<?> startWith(ErrorStream errStream, ExecutorService taskExecutor) {
        try {
            Future<?> future = new KnownFuture<>(new Object());
            return newComposer(future, errStream, taskExecutor);
        } catch (Throwable t) {
            errStream.onError(t);
            return newComposer(null, errStream, taskExecutor);
        }
    }

    /**
     * <p>Factory method to create new composer instance with some pre-known value and custom executor service.</p>
     * <p>Use only in the cases you need to specify custom executor service to execute tasks.</p>
     *
     * @param value        pre-known value to begin with
     * @param errStream    consumer for all errors.
     * @param taskExecutor executor service with custom thread pool to submit the task.
     * @param <R>          type of task output.
     * @return new composer instance.
     * @see #startWith(Object, ErrorStream)
     */
    public static <R> Composable<R> startWith(R value, ErrorStream errStream, ExecutorService taskExecutor) {
        try {
            Future<R> future = new KnownFuture<>(value);
            return newComposer(future, errStream, taskExecutor);
        } catch (Throwable t) {
            errStream.onError(t);
            return newComposer(null, errStream, taskExecutor);
        }
    }

    /**
     * <p>Factory method to create new composer instance with asynchronous runnable task and custom executor service.</p>
     * <p>Use only in the cases you need to specify custom executor service to execute tasks.</p>
     *
     * @param task         task which takes no input and returns no output
     * @param errStream    consumer for all errors.
     * @param taskExecutor executor service with custom thread pool to submit the task.
     * @return new composer instance.
     * @see #startWith(SimpleTask, ErrorStream)
     */
    public static Composable<?> startWith(SimpleTask task, ErrorStream errStream, ExecutorService taskExecutor) {
        try {
            Future<?> future = taskExecutor.submit(() -> {
                try {
                    task.execute();
                } catch (Exception e) {
                    throw new ComposerException(e);
                }
            });
            return newComposer(future, errStream, taskExecutor);
        } catch (Throwable t) {
            errStream.onError(t);
            return newComposer(null, errStream, taskExecutor);
        }
    }

    /**
     * <p>Factory method to create new composer instance with asynchronous producer task and custom executor service.</p>
     * <p>Use only in the cases you need to specify custom executor service to execute tasks.</p>
     *
     * @param task         task which produces an output
     * @param errStream    consumer for all errors.
     * @param taskExecutor executor service with custom thread pool to submit the task.
     * @param <R>          type of task output.
     * @return new composer instance.
     * @see #startWith(ProducingTask, ErrorStream)
     */
    public static <R> Composable<R> startWith(ProducingTask<R> task, ErrorStream errStream, ExecutorService taskExecutor) {
        try {
            Future<R> future = taskExecutor.submit(task::produce);
            return newComposer(future, errStream, taskExecutor);
        } catch (Throwable t) {
            errStream.onError(t);
            return newComposer(null, errStream, taskExecutor);
        }
    }
    //endregion

    //region private API
    private static <R> Composer<R> newComposer(Future<R> future, ErrorStream errStream, ExecutorService executorService) {
        return new Composer<>(future, errStream, executorService);
    }

    //region public API
    @Override
    public Composable<T> thenExecute(SimpleTask task) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);
            Future<T> resultFuture = async(() -> uncheckedTask(task), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenExecuteTogether(SimpleTask... tasks) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<T> resultFuture = deferred(() -> uncheckedTask(() -> {
                CountDownLatch latch = newLatch(tasks.length);
                for (SimpleTask task : tasks) {
                    async(() -> latchedTask(() -> uncheckedTask(task), latch));
                }
                latch.await();
            }), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenWaitFor(SimpleTask task) {
        return chainWith(() -> {
            await();
            task.execute();
            return this;
        });
    }

    @Override
    public Composable<T> thenExecute(ConsumingTask<T> task) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);
            Future<T> resultFuture = async(() -> uncheckedTask(task, upstream), upstream);
            return switchTo(resultFuture);
        });
    }

    @SafeVarargs
    @Override
    public final Composable<T> thenExecuteTogether(ConsumingTask<T>... tasks) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<T> resultFuture = deferred(() -> uncheckedTask(() -> {
                CountDownLatch latch = newLatch(tasks.length);
                for (ConsumingTask<T> task : tasks) {
                    async(() -> latchedTask(() -> uncheckedTask(task, upstream), latch));
                }
                latch.await();
            }), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S> Composable<T> thenExecuteForEach(Distributor<T, Collection<S>> distributor, ConsumingTask<S> task) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Collection<S> collection = distributor.distribute(upstream);
            if (collection == null) return switchTo(null);

            Future<T> resultFuture = deferred(() -> uncheckedTask(() -> {
                CountDownLatch latch = newLatch(collection.size());
                for (S value : collection) {
                    async(() -> latchedTask(() -> uncheckedTask(task, value), latch));
                }
                latch.await();
            }), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenWaitFor(ConsumingTask<T> task) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);
            task.consume(upstream);
            return this;
        });
    }

    @Override
    public <R> Composable<R> thenExecute(ProducingTask<R> task) {
        return chainWith(() -> {
            await();
            Future<R> future = async(task::produce);
            return switchTo(future);
        });
    }

    @SafeVarargs
    @Override
    public final <S, R> Composable<R> thenExecuteTogether(Collector<T, Set<S>, R> resultsCollector, ProducingTask<S>... tasks) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(tasks.length);
                Set<Future<S>> futures = new LinkedHashSet<>();
                for (ProducingTask<S> task : tasks) {
                    Future<S> future = async(() -> latchedTask(task::produce, latch));
                    futures.add(future);
                }
                latch.await();

                Set<S> results = new LinkedHashSet<>();
                for (Future<S> future : futures) {
                    S result = future.get();
                    results.add(result);
                }
                return resultsCollector.collect(upstream, results);
            });
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, R> Composable<R> thenExecuteTogether(ProducingTask<S> task1, ProducingTask<U> task2, BiCollector<T, S, U, R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(2);
                Future<S> future1 = async(() -> latchedTask(task1::produce, latch));
                Future<U> future2 = async(() -> latchedTask(task2::produce, latch));
                latch.await();

                S result1 = future1.get();
                U result2 = future2.get();
                return resultsCollector.collect(upstream, result1, result2);
            });
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenExecuteTogether(ProducingTask<S> task1, ProducingTask<U> task2, ProducingTask<V> task3, TriCollector<T, S, U, V, R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(3);
                Future<S> future1 = async(() -> latchedTask(task1::produce, latch));
                Future<U> future2 = async(() -> latchedTask(task2::produce, latch));
                Future<V> future3 = async(() -> latchedTask(task3::produce, latch));
                latch.await();

                S result1 = future1.get();
                U result2 = future2.get();
                V result3 = future3.get();
                return resultsCollector.collect(upstream, result1, result2, result3);
            });
            return switchTo(resultFuture);
        });
    }

    @Override
    public <R> Composable<R> thenWaitFor(ProducingTask<R> task) {
        return chainWith(() -> {
            await();
            R result = task.produce();
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <R> Composable<R> thenExecute(TransformingTask<T, R> task) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);
            Future<R> future = async(() -> task.transform(upstream));
            return switchTo(future);
        });
    }

    @SafeVarargs
    @Override
    public final <S, R> Composable<R> thenExecuteTogether(Collector<T, Set<S>, R> resultsCollector, TransformingTask<T, S>... tasks) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(tasks.length);

                List<Future<S>> futures = new ArrayList<>();
                for (TransformingTask<T, S> task : tasks) {
                    Future<S> future = async(() -> latchedTask(() -> task.transform(upstream), latch));
                    futures.add(future);
                }
                latch.await();

                Set<S> results = new LinkedHashSet<>();
                for (Future<S> future : futures) {
                    S result = future.get();
                    results.add(result);
                }
                return resultsCollector.collect(upstream, results);
            });
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, R> Composable<R> thenExecuteTogether(TransformingTask<T, S> task1, TransformingTask<T, U> task2, BiCollector<T, S, U, R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(2);
                Future<S> future1 = async(() -> latchedTask(() -> task1.transform(upstream), latch));
                Future<U> future2 = async(() -> latchedTask(() -> task2.transform(upstream), latch));
                latch.await();

                S result1 = future1.get();
                U result2 = future2.get();
                return resultsCollector.collect(upstream, result1, result2);
            });
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenExecuteTogether(TransformingTask<T, S> task1, TransformingTask<T, U> task2, TransformingTask<T, V> task3, TriCollector<T, S, U, V, R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(3);
                Future<S> future1 = async(() -> latchedTask(() -> task1.transform(upstream), latch));
                Future<U> future2 = async(() -> latchedTask(() -> task2.transform(upstream), latch));
                Future<V> future3 = async(() -> latchedTask(() -> task3.transform(upstream), latch));
                latch.await();

                S result1 = future1.get();
                U result2 = future2.get();
                V result3 = future3.get();
                return resultsCollector.collect(upstream, result1, result2, result3);
            });
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, R> Composable<R> thenExecuteForEach(Distributor<T, Collection<S>> distributor, TransformingTask<S, U> task, Collector<T, Set<Pair<S, U>>, R> resultsCollector) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);

            Collection<S> collection = distributor.distribute(upstream);
            if (collection == null) return switchTo(null);

            Future<R> resultFuture = deferred(() -> {
                CountDownLatch latch = newLatch(collection.size());

                Set<Pair<S, Future<U>>> futures = new LinkedHashSet<>();
                for (S value : collection) {
                    Future<U> future = async(() -> latchedTask(() -> task.transform(value), latch));
                    futures.add(Pair.create(value, future));
                }
                latch.await();

                Set<Pair<S, U>> results = new LinkedHashSet<>();
                for (Pair<S, Future<U>> future : futures) {
                    U result = future.getValue().get();
                    results.add(Pair.create(future.getKey(), result));
                }
                return resultsCollector.collect(upstream, results);
            });
            return switchTo(resultFuture);

        });
    }

    @Override
    public <R> Composable<R> thenWaitFor(TransformingTask<T, R> task) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);
            R result = task.transform(upstream);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public Composable<T> thenContinueIf(Validator<T> validator) {
        return chainWith(() -> {
            T upstream = await();
            if (upstream == null) return switchTo(null);
            if (validator.validate(upstream)) {
                return this;
            } else {
                errStream.onError(new ComposerException(String.format("The upstream value: %s is not valid as per the condition specified in given validator: %s "
                        + "\nDownstream execution will stop now.", upstream, validator)));
                return switchTo(null);
            }
        });
    }

    @Override
    public void thenFinish() {
        try {
            await();
        } catch (Throwable t) {
            errStream.onError(t);
        }
    }

    @Override
    public void thenFinish(ConsumingTask<T> upstreamResultConsumer) {
        try {
            upstreamResultConsumer.consume(await());
        } catch (Throwable t) {
            errStream.onError(t);
        }
    }
    //endregion

    private <R> Composer<R> switchTo(Future<R> resultFuture) {
        return newComposer(resultFuture, this.errStream, this.taskExecutor);
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

    private <R> Future<R> deferred(Callable<R> step) {
        return PARENT_EXECUTOR.submit(step);
    }

    private <R> Future<R> deferred(Runnable step, R result) {
        return PARENT_EXECUTOR.submit(step, result);
    }

    private <R> Future<R> async(Callable<R> task) {
        return taskExecutor.submit(task);
    }

    private <R> Future<R> async(Runnable task, R result) {
        return taskExecutor.submit(task, result);
    }

    private void async(Runnable task) {
        taskExecutor.submit(task);
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

    private <R> R latchedTask(Callable<R> task, CountDownLatch latch) throws Exception {
        try {
            return task.call();
        } finally {
            latch.countDown();
        }
    }

    private void latchedTask(Runnable task, CountDownLatch latch) {
        try {
            task.run();
        } finally {
            latch.countDown();
        }
    }

    private void uncheckedTask(SimpleTask task) {
        try {
            task.execute();
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
