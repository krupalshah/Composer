package com.krupalshah.composer.function.collector;

import java.util.Collection;

@FunctionalInterface
public interface Collector<S, T extends Collection, R> {
    R collect(S upstream, T results);
}