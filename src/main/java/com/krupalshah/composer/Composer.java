package com.krupalshah.composer;

import com.krupalshah.composer.exception.ComposeException;
import com.krupalshah.composer.function.*;
import com.krupalshah.composer.util.KnownFuture;

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
     *
     * @param task        first asynchronous task to begin with which returns a result.
     * @param errConsumer consume for all errors.
     * @param <R>         return type of task.
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
            await();
            Future<R> future = async(task);
            return switchTo(future);
        });
    }

    @Override
    public <S, R> Composable<R> thenCall(Set<Callable<? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner) {
        return chainWith(() -> {
            await();

            CountDownLatch latch = newLatch(tasks.size());
            Set<Future<? extends S>> futures = new LinkedHashSet<>();
            for (Callable<? extends S> task : tasks) {
                Future<? extends S> future = async(() -> latchWrappedTask(task, latch));
                futures.add(future);
            }
            latch.await();

            Set<S> results = new LinkedHashSet<>();
            for (Future<? extends S> future : futures) {
                S result = future.get();
                results.add(result);
            }
            Future<R> resultFuture = async(() -> resultCombiner.apply(results));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, R> Composable<R> thenCall(Callable<? extends S> firstTask, Callable<? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner) {
        return chainWith(() -> {
            await();

            CountDownLatch latch = newLatch(2);
            Future<? extends S> firstF = async(() -> latchWrappedTask(firstTask, latch));
            Future<? extends U> secondF = async(() -> latchWrappedTask(secondTask, latch));
            latch.await();

            S first = firstF.get();
            U second = secondF.get();
            Future<R> resultFuture = async(() -> resultCombiner.apply(first, second));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenCall(Callable<? extends S> firstTask, Callable<? extends U> secondTask, Callable<? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner) {
        return chainWith(() -> {
            await();

            CountDownLatch latch = newLatch(3);
            Future<? extends S> firstF = async(() -> latchWrappedTask(firstTask, latch));
            Future<? extends U> secondF = async(() -> latchWrappedTask(secondTask, latch));
            Future<? extends V> thirdF = async(() -> latchWrappedTask(thirdTask, latch));
            latch.await();

            S first = firstF.get();
            U second = secondF.get();
            V third = thirdF.get();
            Future<R> resultFuture = async(() -> resultCombiner.apply(first, second, third));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <R> Composable<R> thenCallSynchronously(Callable<R> task) {
        return chainWith(() -> {
            await();
            R result = task.call();
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public <R> Composable<R> thenTransform(Function<? super T, ? extends R> task) {
        return chainWith(() -> {
            T upstream = await();
            Future<R> future = async(() -> task.apply(upstream));
            return switchTo(future);
        });
    }

    @Override
    public <S, R> Composable<R> thenTransform(Set<Function<? super T, ? extends S>> tasks, Function<Set<? super S>, ? extends R> resultCombiner) {
        return chainWith(() -> {
            T upstream = await();

            CountDownLatch latch = newLatch(tasks.size());
            Set<Future<? extends S>> futures = new LinkedHashSet<>();
            for (Function<? super T, ? extends S> task : tasks) {
                Future<? extends S> future = async(() -> latchWrappedTask(() -> task.apply(upstream), latch));
                futures.add(future);
            }
            latch.await();

            Set<S> results = new LinkedHashSet<>();
            for (Future<? extends S> future : futures) {
                S result = future.get();
                results.add(result);
            }
            Future<R> resultFuture = async(() -> resultCombiner.apply(results));
            return switchTo(resultFuture);
        });
    }


    @Override
    public <S, U, R> Composable<R> thenTransform(Function<? super T, ? extends S> firstTask, Function<? super T, ? extends U> secondTask, BiFunction<? super S, ? super U, ? extends R> resultCombiner) {
        return chainWith(() -> {
            T upstream = await();

            CountDownLatch latch = newLatch(2);
            Future<? extends S> firstF = async(() -> latchWrappedTask(() -> firstTask.apply(upstream), latch));
            Future<? extends U> secondF = async(() -> latchWrappedTask(() -> secondTask.apply(upstream), latch));
            latch.await();

            S first = firstF.get();
            U second = secondF.get();
            Future<R> resultFuture = async(() -> resultCombiner.apply(first, second));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <S, U, V, R> Composable<R> thenTransform(Function<? super T, ? extends S> firstTask, Function<? super T, ? extends U> secondTask, Function<? super T, ? extends V> thirdTask, TriFunction<? super S, ? super U, ? super V, ? extends R> resultCombiner) {
        return chainWith(() -> {
            T upstream = await();

            CountDownLatch latch = newLatch(3);
            Future<? extends S> firstF = async(() -> latchWrappedTask(() -> firstTask.apply(upstream), latch));
            Future<? extends U> secondF = async(() -> latchWrappedTask(() -> secondTask.apply(upstream), latch));
            Future<? extends V> thirdF = async(() -> latchWrappedTask(() -> thirdTask.apply(upstream), latch));
            latch.await();

            S first = firstF.get();
            U second = secondF.get();
            V third = thirdF.get();
            Future<R> resultFuture = async(() -> resultCombiner.apply(first, second, third));
            return switchTo(resultFuture);
        });
    }

    @Override
    public <R> Composable<R> thenTransformSynchronously(Function<? super T, ? extends R> task) {
        return chainWith(() -> {
            T upstream = await();
            R result = task.apply(upstream);
            return switchTo(new KnownFuture<>(result));
        });
    }

    @Override
    public Composable<T> thenConsume(Consumer<T> task) {
        return chainWith(() -> {
            T upstream = await();
            Future<T> resultFuture = async(() -> task.accept(upstream), upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenConsume(Set<Consumer<T>> tasks) {
        return chainWith(() -> {
            T upstream = await();
            CountDownLatch latch = newLatch(tasks.size());
            for (Consumer<T> task : tasks) {
                async(() -> latchWrappedTask(() -> task.accept(upstream), latch));
            }
            latch.await();
            return this;
        });
    }

    @Override
    public Composable<T> thenConsumeSynchronously(Consumer<T> task) {
        return chainWith(() -> {
            T upstream = await();
            task.accept(upstream);
            return this;
        });
    }


    @Override
    public Composable<T> thenExecute(Runnable task) {
        return chainWith(() -> {
            T upstream = await();
            Future<T> resultFuture = async(task, upstream);
            return switchTo(resultFuture);
        });
    }

    @Override
    public Composable<T> thenExecute(Set<Runnable> tasks) {
        return chainWith(() -> {
            await();
            CountDownLatch latch = newLatch(tasks.size());
            for (Runnable task : tasks) {
                async(() -> latchWrappedTask(task, latch));
            }
            latch.await();
            return this;
        });
    }

    @Override
    public Composable<T> thenExecuteSynchronously(Runnable task) {
        return chainWith(() -> {
            await();
            task.run();
            return this;
        });
    }

    @Override
    public Composable<T> thenCheckIf(Predicate<? super T> validator) {
        return chainWith(() -> {
            T upstream = await();
            if (validator.test(upstream)) {
                return this;
            } else {
                errConsumer.accept(new ComposeException(String.format("The upstream result %s " +
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
