package ru.ifmo.rain.akimov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> runningThreads;
    private final Scheduler scheduler;

    private static class Result<T> {
        private int remaining;
        private RuntimeException finalException;
        private final List<T> ans;

        public Result(int size) {
            ans = new ArrayList<>(Collections.nCopies(size, null));
            remaining = size;
            finalException = null;
        }

        private synchronized void setResult(final int index, final T result) {
            ans.set(index, result);
        }

        private synchronized void setException(RuntimeException e) {
            if (finalException == null) {
                finalException = e;
            } else {
                finalException.addSuppressed(e);
            }
        }

        public <R> void set(final int index, final R arg, final Function<? super R, ? extends T> f) {
            try {
                setResult(index, f.apply(arg));
            } catch (RuntimeException e) {
                setException(e);
            }
            synchronized (this) {
                remaining--;
                if (remaining == 0) {
                    notify();
                }
            }
        }

        public synchronized List<T> getAns() throws InterruptedException, RuntimeException {
            while (remaining != 0) {
                wait();
            }
            if (finalException != null) {
                throw finalException;
            }
            return ans;
        }
    }

    private static class Scheduler {
        private final Queue<Runnable> runs;

        public Scheduler() {
            runs = new ArrayDeque<>();
        }

        public synchronized Runnable getTask() throws InterruptedException {
            while (runs.isEmpty()) {
                wait();
            }
            return runs.poll();
        }

        public synchronized <T, R> void addTasks(List<T> args, Result<R> result, Function<? super T, ? extends R> f) {
            for (int i = 0; i < args.size(); i++) {
                final int index = i;
                final T arg = args.get(i);
                runs.add(() -> result.set(index, arg, f));
            }
            notifyAll();
        }
    }

    private void work() {
        try {
            while (!Thread.interrupted()) {
                scheduler.getTask().run();
            }
        } catch (InterruptedException ignored) {
        } finally {
            Thread.currentThread().interrupt();
        }
    }

    public ParallelMapperImpl(final int threads) {
        scheduler = new Scheduler();
        runningThreads = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            runningThreads.add(new Thread(this::work));
            runningThreads.get(i).start();
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        Result<R> result = new Result<>(args.size());
        scheduler.addTasks(args, result, f);
        return result.getAns();
    }

    @Override
    public void close() {
        runningThreads.forEach(Thread::interrupt);
        for (Thread th : runningThreads) {
            try {
                th.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
