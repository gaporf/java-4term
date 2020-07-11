package ru.ifmo.rain.akimov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    private <T> List<Stream<T>> divide(int n, List<T> values) {
        List<T> optList = (values instanceof RandomAccess) ? values : new ArrayList<>(values);
        int rest = optList.size();
        List<Stream<T>> ans = new ArrayList<>();
        for (int i = 0; i < optList.size(); ) {
            final int k = rest / n;
            n--;
            rest -= k;
            ans.add(optList.subList(i, i + k).stream());
            i += k;
        }
        return ans;
    }

    private <T, U> List<U> parodyToParallelMapper(Function<Stream<T>, U> function, List<Stream<T>> streams, int threads) throws InterruptedException {
        List<Thread> runningThreads = new ArrayList<>(threads);
        List<U> newValues = new ArrayList<>(Collections.nCopies(threads, null));
        for (Stream<T> stream : streams) {
            final int num = runningThreads.size();
            runningThreads.add(new Thread(() -> {
                U ans = function.apply(stream);
                synchronized (newValues) {
                    newValues.set(num, ans);
                }
            }));
            runningThreads.get(num).start();
        }
        List<InterruptedException> exceptionsByThreads = new ArrayList<>();
        boolean isInterrupt = false;
        for (Thread runningThread : runningThreads) {
            try {
                if (isInterrupt) {
                    runningThread.interrupt();
                }
                runningThread.join();
            } catch (InterruptedException e) {
                exceptionsByThreads.add(e);
                isInterrupt = true;
            }
        }
        if (!exceptionsByThreads.isEmpty()) {
            InterruptedException e = new InterruptedException();
            for (Exception exc : exceptionsByThreads) {
                e.addSuppressed(exc);
            }
            throw e;
        } else {
            return newValues;
        }
    }

    private <T, U> List<U> parallel(int threads, List<T> values, Function<Stream<T>, U> function) throws InterruptedException {
        threads = Math.min(threads, values.size());
        List<Stream<T>> streams = divide(threads, values);
        if (parallelMapper == null) {
            return parodyToParallelMapper(function, streams, threads);
        } else {
            return parallelMapper.map(function, streams);
        }
    }

    private <T> T repeat(int threads, List<T> values, Function<Stream<T>, T> function) throws InterruptedException {
        return function.apply(parallel(threads, values, function).stream());
    }

    public IterativeParallelism() {
        parallelMapper = null;
    }

    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @return maximum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("Empty list of values");
        }
        return repeat(threads, values, (stream -> stream.max(comparator).orElseThrow()));
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @return minimum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallel(threads, values, (stream -> stream.allMatch(predicate))).stream().reduce(true, Boolean::logicalAnd);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return String.join("", parallel(threads, values, (stream -> stream.map(Object::toString).collect(Collectors.joining()))));
    }

    private <T, U> List<U> getList(int threads, List<T> values, Function<Stream<T>, List<U>> function) throws InterruptedException {
        List<List<U>> newValues = parallel(threads, values, function);
        List<U> ans = new ArrayList<>();
        for (List<U> newValue : newValues) {
            ans.addAll(newValue);
        }
        return ans;
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getList(threads, values, (stream -> stream.filter(predicate).collect(Collectors.toList())));
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return getList(threads, values, (stream -> stream.map(f).collect(Collectors.toList())));
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return repeat(threads, values, (stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator())));
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param values  values to reduce.
     * @param lift    mapping function.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return reduce(threads, map(threads, values, lift), monoid);
    }
}
