package com.krupalshah.composer.operators;

import com.krupalshah.composer.Composable;
import com.krupalshah.composer.function.BiFunction;
import com.krupalshah.composer.function.Function;

public class With<In, Out, Combined> {
    final Function<In, Out> operation;
    final BiFunction<In, Out, Combined> combiner;
    final Composable<Combined> destination;

    public With(Function<In, Out> operation, BiFunction<In, Out, Combined> combiner, Composable<Combined> destination) {
        this.operation = operation;
        this.combiner = combiner;
        this.destination = destination;
    }

}
