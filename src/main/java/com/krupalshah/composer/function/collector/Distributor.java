package com.krupalshah.composer.function.collector;

import java.util.Collection;

@FunctionalInterface
public interface Distributor<T, S extends Collection> {
    S distribute(T result);
}
