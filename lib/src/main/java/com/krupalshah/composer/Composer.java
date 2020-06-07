package com.krupalshah.composer;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Composer<In> {

    <Out> Composer<Out> then(Function<In, Out> asyncOperation);

    <Out, Combined> Composer<Combined> with(Function<In, Out> asyncOperation, BiFunction<In, Out, Combined> combiner);

    <Element> Composer<Element> stream(Function<In, Iterator<Element>> iteratorSupplier);

    <Out extends Iterable<In>> Composer<Out> collect(Function<In, Out> accumulator);

    <Ex extends Exception> void execute(Consumer<In> resultConsumer, Consumer<Ex> exceptionConsumer);
}
