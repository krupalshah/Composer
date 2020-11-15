package com.krupalshah.composer.exception;

@FunctionalInterface
public interface ErrorStream {
    void onError(Throwable t);
}
