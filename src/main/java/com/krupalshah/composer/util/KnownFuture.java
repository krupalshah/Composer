package com.krupalshah.composer.util;


import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public  class KnownFuture<T> implements Future<T> {

    private T result;

    public KnownFuture(T result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() {
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        return result;
    }
}
