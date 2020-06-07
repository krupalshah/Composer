package com.krupalshah.composer.operators;

import com.krupalshah.composer.Composer;
import com.krupalshah.composer.function.Function;

public class Then<In,Out> {
    final Function<In,Out> operation;
    final Composer<Out> destination;

    public Then(Function<In, Out> operation, Composer<Out> destination) {
        this.operation = operation;
        this.destination = destination;
    }
}
