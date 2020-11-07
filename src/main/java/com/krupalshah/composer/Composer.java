package com.krupalshah.composer;

import com.krupalshah.composer.exception.ComposeException;
import com.krupalshah.composer.function.*;

import java.util.concurrent.*;

public class Composer<T> implements Composable<T> {

    private final Future<T> future;
    private final Consumer<Throwable> errConsumer;
    private final ExecutorService executorService;

    private Composer(Future<T> future, Consumer<Throwable> errConsumer, ExecutorService executorService) {
        this.future = future;
        this.errConsumer = errConsumer;
        this.executorService = executorService;
    }

    public static <R> Composer<R> startWith(Callable<R> task, Consumer<Throwable> errConsumer) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            Future<R> future = executorService.submit(task);
            return new Composer<>(future, errConsumer, executorService);
        } catch (Throwable t) {
            errConsumer.accept(t);
            return new Composer<>(null, errConsumer, executorService);
        }
    }

    public static <R> Composer<R> startWith(Callable<R> task, Consumer<Throwable> errConsumer, ExecutorService executorService) {
        try {
            Future<R> future = executorService.submit(task);
            return new Composer<>(future, errConsumer, executorService);
        } catch (Throwable t) {
            errConsumer.accept(t);
            return new Composer<>(null, errConsumer, executorService);
        }
    }

    @Override
    public <R> Composable<R> thenCall(Callable<R> task) {
        return chainWith(() -> {
            awaitResult();
            Future<R> future = executorService.submit(task);
            return newComposer(future);
        });
    }

    @Override
    public <S, U, R> Composable<R> thenCallTogether(Callable<S> firstTask, Callable<U> secondTask, BiFunction<S, U, R> resultCombiner) {
        return chainWith(() -> {
            awaitResult();

            CountDownLatch latch = newLatch(2);
            Future<S> future1 = executorService.submit(() -> latchWrappedTask(firstTask, latch));
            Future<U> future2 = executorService.submit(() -> latchWrappedTask(secondTask, latch));
            S s = future1.get();
            U u = future2.get();
            latch.await();

            Future<R> resultFuture = executorService.submit(() -> resultCombiner.apply(s, u));
            return newComposer(resultFuture);
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenCallTogether(Callable<S> firstTask, Callable<U> secondTask, Callable<V> thirdTask, TriFunction<S, U, V, R> resultCombiner) {
        return chainWith(() -> {
            awaitResult();

            CountDownLatch latch = newLatch(3);
            Future<S> future1 = executorService.submit(() -> latchWrappedTask(firstTask, latch));
            Future<U> future2 = executorService.submit(() -> latchWrappedTask(secondTask, latch));
            Future<V> future3 = executorService.submit(() -> latchWrappedTask(thirdTask, latch));
            S s = future1.get();
            U u = future2.get();
            V v = future3.get();
            latch.await();

            Future<R> resultFuture = executorService.submit(() -> resultCombiner.apply(s, u, v));
            return newComposer(resultFuture);
        });
    }

    @Override
    public Composable<T> thenExecute(Runnable task) {
        return chainWith(() -> {
            T t = awaitResult();
            Future<T> resultFuture = executorService.submit(task, t);
            return newComposer(resultFuture);
        });
    }

    @Override
    public Composable<T> thenRunSynchronously(Runnable task) {
        return chainWith(() -> {
            awaitResult();
            task.run();
            return newComposer(this.future);
        });
    }

    @Override
    public <R> Composable<R> thenProcess(Function<T, R> processor) {
        return chainWith(() -> {
            T t = awaitResult();
            Future<R> future = executorService.submit(() -> processor.apply(t));
            return newComposer(future);
        });
    }

    @Override
    public Composable<T> thenCheck(Predicate<T> validator) {
        return chainWith(() -> {
            T t = awaitResult();
            if (validator.test(t)) {
                return newComposer(this.future);
            } else {
                errConsumer.accept(new ComposeException(String.format("The upstream result %s " +
                        "is not valid as per the validator provided! Downstream execution will stop now.", t)));
                return newComposer(null);
            }
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

    private <R> Composer<R> chainWith(Callable<Composer<R>> composerSupplier) {
        if (this.future == null) {
            return newComposer(null);
        } else {
            try {
                return composerSupplier.call();
            } catch (Throwable t) {
                errConsumer.accept(t);
                return newComposer(null);
            }
        }
    }

    private <R> Composer<R> newComposer(Future<R> resultFuture) {
        return new Composer<>(resultFuture, this.errConsumer, this.executorService);
    }

    private CountDownLatch newLatch(int nTasks) {
        return new CountDownLatch(nTasks);
    }

    private T awaitResult() throws InterruptedException, ExecutionException {
        if (this.future == null) {
            return null;
        }
        return this.future.get();
    }

    private <R> R latchWrappedTask(Callable<R> task, CountDownLatch latch) throws Exception {
        try {
            return task.call();
        } finally {
            latch.countDown();
        }
    }

}
