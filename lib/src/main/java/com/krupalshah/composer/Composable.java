package com.krupalshah.composer;

import com.krupalshah.composer.function.BiFunction;
import com.krupalshah.composer.function.Consumer;
import com.krupalshah.composer.function.Function;
import com.krupalshah.composer.function.Supplier;

import java.util.Iterator;

public interface Composable<In> {

    <Out> Composable<Out> then(Function<In, Out> asyncOperation);

    <Out, Combined> Composable<Combined> with(Function<In, Out> asyncOperation, BiFunction<In, Out, Combined> combiner);

    <Element> Composable<Element> stream(Function<In, Iterator<Element>> iteratorSupplier);

    <Out extends Iterable<In>> Composable<Out> collect(Supplier<Out> accumulator);

    <Ex extends Exception> void execute(Consumer<In> resultConsumer, Consumer<Ex> exceptionConsumer);
}
