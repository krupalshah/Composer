package com.krupalshah.composer.function.collector;

import java.util.Collection;

@FunctionalInterface
public interface Expander<T, S extends Collection> {
    S expand(T result);
}
