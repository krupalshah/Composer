package com.krupalshah.composer.function.collector;

import java.util.Collection;

@FunctionalInterface
public interface Collector<T extends Collection, R> {
    R collect(T results);
}