package com.krupalshah.composer;

import com.krupalshah.composer.function.BiFunction;
import com.krupalshah.composer.function.Callable;
import com.krupalshah.composer.function.Consumer;
import com.krupalshah.composer.function.Function;
import com.krupalshah.composer.function.Supplier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class Composer<In> implements Composable<In> {

    private static List<Object> operations = new ArrayList<>();

    public static <Out> Composable<Out> compose(Callable<Out> asyncOperation) {
        return null;
    }

    @Override
    public <Out> Composable<Out> then(Function<In, Out> asyncOperation) {
        return null;
    }

    @Override
    public <Out, Combined> Composable<Combined> with(Function<In, Out> asyncOperation, BiFunction<In, Out, Combined> combiner) {
        return null;
    }

    @Override
    public <Element> Composable<Element> stream(Function<In, Iterator<Element>> iteratorSupplier) {
        return null;
    }

    @Override
    public <Out extends Iterable<In>> Composable<Out> collect(Supplier<Out> accumulator) {
        return null;
    }

    @Override
    public <Ex extends Exception> void execute(Consumer<In> resultConsumer, Consumer<Ex> exceptionConsumer) {

    }
}
