package com.krupalshah.composer.operators;

import com.krupalshah.composer.Composable;
import com.krupalshah.composer.function.Function;

public class Then<In, Out> {
    final Function<In, Out> operation;
    final Composable<Out> destination;

    public Then(Function<In, Out> operation, Composable<Out> destination) {
        this.operation = operation;
        this.destination = destination;
    }

}
