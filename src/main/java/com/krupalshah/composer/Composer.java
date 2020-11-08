package com.krupalshah.composer;

import com.krupalshah.composer.exception.ComposeException;
import com.krupalshah.composer.function.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Implementation of {@link Composable} and serves as an entry point for all composer functions.
 * Use factory methods {@link #startWith(Callable, Consumer)} to create new instance.
 */
public class Composer<T> implements Composable<T> {

    private final Future<T> future;
    private final Consumer<Throwable> errConsumer;
    private final ExecutorService executorService;

    private Composer(Future<T> future, Consumer<Throwable> errConsumer, ExecutorService executorService) {
        this.future = future;
        this.errConsumer = errConsumer;
        this.executorService = executorService;
    }

    /**
     * Convenient factory method to create new composer instance.
     * Creates default {@link ExecutorService} using cached thread pool internally for returned composer instance.
     * @param task first asynchronous task to begin with which returns a result.
     * @param errConsumer consume for all errors.
     * @param <R> return type of task.
     * @return new composer instance.
     */
    public static <R> Composer<R> startWith(Callable<R> task, Consumer<Throwable> errConsumer) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        return startWith(task, errConsumer, executorService);
    }

    public static <R> Composer<R> startWith(Callable<R> task, Consumer<Throwable> errConsumer, ExecutorService executorService) {
        try {
            Future<R> future = executorService.submit(task);
            return newComposer(future, errConsumer, executorService);
        } catch (Throwable t) {
            errConsumer.accept(t);
            return newComposer(null, errConsumer, executorService);
        }
    }

    @Override
    public <R> Composable<R> thenCall(Callable<R> task) {
        return chainWith(() -> {
            awaitResult();
            Future<R> future = executorService.submit(task);
            return switchTo(future);
        });
    }

    @Override
    public <S, R> Composable<R> thenCallTogether(Set<Callable<? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner) {
        return chainWith(() -> {
            awaitResult();

            CountDownLatch latch = newLatch(tasks.size());
            Set<Future<? extends S>> futures = new LinkedHashSet<>();
            for (Callable<? extends S> task : tasks) {
                Future<? extends S> submittedFuture = executorService.submit(() -> latchWrappedTask(task, latch));
                futures.add(submittedFuture);
            }
            latch.await();

            Set<S> results = new LinkedHashSet<>();
            for (Future<? extends S> future : futures) {
                S result = future.get();
                results.add(result);
            }
            Future<R> resultFuture = executorService.submit(() -> resultCombiner.apply(results));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, R> Composable<R> thenCallTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner) {
        return chainWith(() -> {
            awaitResult();

            CountDownLatch latch = newLatch(2);
            Future<? extends S> firstF = executorService.submit(() -> latchWrappedTask(firstTask, latch));
            Future<? extends U> secondF = executorService.submit(() -> latchWrappedTask(secondTask, latch));
            latch.await();

            S first = firstF.get();
            U second = secondF.get();
            Future<R> resultFuture = executorService.submit(() -> resultCombiner.apply(first, second));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenCallTogether(Callable<? extends S> firstTask, Callable<? extends U> secondTask, Callable<? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner) {
        return chainWith(() -> {
            awaitResult();

            CountDownLatch latch = newLatch(3);
            Future<? extends S> firstF = executorService.submit(() -> latchWrappedTask(firstTask, latch));
            Future<? extends U> secondF = executorService.submit(() -> latchWrappedTask(secondTask, latch));
            Future<? extends V> thirdF = executorService.submit(() -> latchWrappedTask(thirdTask, latch));
            latch.await();

            S first = firstF.get();
            U second = secondF.get();
            V third = thirdF.get();
            Future<R> resultFuture = executorService.submit(() -> resultCombiner.apply(first, second, third));
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenExecute(Runnable task) {
        return chainWith(() -> {
            T result = awaitResult();
            Future<T> resultFuture = executorService.submit(task, result);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenExecuteTogether(Runnable... tasks) {
        return chainWith(() -> {
            awaitResult();
            CountDownLatch latch = newLatch(tasks.length);
            for (Runnable task : tasks) {
                executorService.submit(() -> latchWrappedTask(task, latch));
            }
            latch.await();
            return this;
        });
    }

    @Override
    public Composable<T> thenExecuteSynchronously(Runnable task) {
        return chainWith(() -> {
            awaitResult();
            task.run();
            return this;
        });
    }

    @Override
    public Composable<T> thenCheck(Predicate<? super T> validator) {
        return chainWith(() -> {
            T result = awaitResult();
            if (validator.test(result)) {
                return this;
            } else {
                errConsumer.accept(new ComposeException(String.format("The upstream result %s " +
                        "is not valid as per the validator provided! Downstream execution will stop now.", result)));
                return switchTo(null);
            }
        });
    }

    @Override
    public <R> Composable<R> thenProcess(Function<? super T, ? extends R> processor) {
        return chainWith(() -> {
            T result = awaitResult();
            Future<R> future = executorService.submit(() -> processor.apply(result));
            return switchTo(future);
        });
    }

    public T listen() {
        try {
            return awaitResult();
        } catch (Throwable t) {
            errConsumer.accept(t);
            return null;
        }
    }

    private static <R> Composer<R> newComposer(Future<R> future, Consumer<Throwable> errConsumer, ExecutorService executorService) {
        return new Composer<>(future, errConsumer, executorService);
    }

    private <R> Composer<R> switchTo(Future<R> resultFuture) {
        return newComposer(resultFuture, this.errConsumer, this.executorService);
    }

    private <R> Composer<R> chainWith(Callable<Composer<R>> composerSupplier) {
        if (this.future == null) {
            return switchTo(null);
        } else {
            try {
                return composerSupplier.call();
            } catch (Throwable t) {
                errConsumer.accept(t);
                return switchTo(null);
            }
        }
    }

    private T awaitResult() throws InterruptedException, ExecutionException {
        if (this.future == null) {
            return null;
        }
        return this.future.get();
    }

    private CountDownLatch newLatch(int nTasks) {
        return new CountDownLatch(nTasks);
    }

    private <R> R latchWrappedTask(Callable<R> task, CountDownLatch latch) throws Exception {
        try {
            return task.call();
        } finally {
            latch.countDown();
        }
    }

    private void latchWrappedTask(Runnable task, CountDownLatch latch) {
        try {
            task.run();
        } finally {
            latch.countDown();
        }
    }

}
