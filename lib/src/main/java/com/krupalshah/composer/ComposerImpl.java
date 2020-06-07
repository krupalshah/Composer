package com.krupalshah.composer;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class ComposerImpl<In> implements Composer<In> {

    public static <Out> Composer<Out> compose(Callable<Out> asyncOperation) {
        return null;
    }

    @Override
    public <Out> Composer<Out> then(Function<In, Out> asyncOperation) {
        return null;
    }

    @Override
    public <Out, Combined> Composer<Combined> with(Function<In, Out> asyncOperation, BiFunction<In, Out, Combined> combiner) {
        return null;
    }

    @Override
    public <Element> Composer<Element> stream(Function<In, Iterator<Element>> iteratorSupplier) {
        return null;
    }

    @Override
    public <Out extends Iterable<In>> Composer<Out> collect(Function<In, Out> accumulator) {
        return null;
    }

    @Override
    public <Ex extends Exception> void execute(Consumer<In> resultConsumer, Consumer<Ex> exceptionConsumer) {

    }
}
