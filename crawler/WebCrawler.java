package ru.ifmo.rain.akimov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private static class Query {
        private final Set<String> result;
        private final Map<String, IOException> exceptions;
        private final Set<String> visited;

        public Query() {
            result = ConcurrentHashMap.newKeySet();
            exceptions = new ConcurrentHashMap<>();
            visited = ConcurrentHashMap.newKeySet();
        }

        public boolean tryVisit(final String url) {
            return visited.add(url);
        }

        public void addUrl(final String url) {
            result.add(url);
        }

        public void addException(final String url, final IOException e) {
            exceptions.put(url, e);
        }

        public Result getResult() {
            return new Result(new ArrayList<>(result), new HashMap<>(exceptions));
        }
    }

    private static class Scheduler {
        private final Downloader downloader;
        private final ExecutorService downloaders;
        private final ExecutorService extractors;
        private final int perHost;
        private final Map<String, Host> hosts;

        private static class Host {
            private final Queue<Runnable> busy;
            private final Semaphore semaphore;

            public Host(final int perHost) {
                busy = new LinkedBlockingQueue<>();
                semaphore = new Semaphore(perHost);
            }

            public boolean tryDownload(final Runnable runnable) {
                if (semaphore.tryAcquire()) {
                    return true;
                } else {
                    busy.add(runnable);
                    return false;
                }
            }

            public Runnable getAnotherTask() {
                final Runnable task = busy.poll();
                if (task == null) {
                    semaphore.release();
                }
                return task;
            }
        }

        public Scheduler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
            this.downloader = downloader;
            this.downloaders = Executors.newFixedThreadPool(downloaders);
            this.extractors = Executors.newFixedThreadPool(extractors);
            this.perHost = perHost;
            hosts = new ConcurrentHashMap<>();
        }

        private void download(final String url, final Phaser phaser, final Query query, final Host host, final Queue<String> newLayer, final int depth) {
            try {
                final Document document = downloader.download(url);
                query.addUrl(url);
                if (depth > 1) {
                    extractors.submit(() -> {
                        try {
                            final List<String> urls = document.extractLinks();
                            urls.stream().filter(query::tryVisit).forEach(newLayer::add);
                        } catch (final IOException ignored) {
                        }
                        phaser.arrive();
                    });
                } else {
                    phaser.arrive();
                }
            } catch (final IOException e) {
                query.addException(url, e);
                phaser.arrive();
            }
            final Runnable newTask = host.getAnotherTask();
            if (newTask != null) {
                downloaders.submit(newTask);
            }
        }

        public Queue<String> downloadAndExtract(final Queue<String> curLayer, final Query query, final int depth) {
            final Queue<String> newLayer = new LinkedBlockingQueue<>();
            final Phaser phaser = new Phaser(curLayer.size());
            for (final String url : curLayer) {
                try {
                    final String hostName = URLUtils.getHost(url);
                    final Host host = hosts.computeIfAbsent(hostName, ignored -> new Host(perHost));
                    final Runnable task = () -> download(url, phaser, query, host, newLayer, depth);
                    if (host.tryDownload(task)) {
                        downloaders.submit(task);
                    }
                } catch (final MalformedURLException e) {
                    query.addException(url, e);
                }
            }
            phaser.awaitAdvance(0);
            return newLayer;
        }

        private void shutdown(final ExecutorService executorService) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(100, TimeUnit.MINUTES);
            } catch (final InterruptedException ignored) {
            }
        }

        public void close() {
            shutdown(downloaders);
            shutdown(extractors);
        }
    }

    private final Scheduler scheduler;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        scheduler = new Scheduler(downloader, downloaders, extractors, perHost);
    }

    @Override
    public Result download(final String url, final int depth) {
        final Query query = new Query();
        query.tryVisit(url);
        Queue<String> curLayer = new LinkedBlockingQueue<>(1);
        curLayer.add(url);
        for (int i = 0; i < depth; i++) {
            curLayer = scheduler.downloadAndExtract(curLayer, query, depth - i);
        }
        return query.getResult();
    }

    @Override
    public void close() {
        scheduler.close();
    }

    private static int getOrDefault(final String[] args, final int index, final int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        } else {
            try {
                return Integer.parseInt(args[index]);
            } catch (final NumberFormatException e) {
                return -1;
            }
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length == 0 || args.length > 5) {
            System.err.println("It's necessary to put from 1 to 5 arguments");
            return;
        }
        final String url = args[0];
        final int depth = getOrDefault(args, 1, 2);
        final int downloads = getOrDefault(args, 2, 10);
        final int extractors = getOrDefault(args, 3, 10);
        final int perHost = getOrDefault(args, 4, 5);
        final Downloader downloader;
        try {
            downloader = new CachingDownloader();
        } catch (final IOException e) {
            System.err.println("Could not create downloader");
            return;
        }
        if (depth == -1 || downloads == -1 || extractors == -1 || perHost == -1) {
            System.err.println("Invalid arguments, expected numbers!");
        } else {
            try (final WebCrawler webCrawler = new WebCrawler(downloader, downloads, extractors, perHost)) {
                final Result result = webCrawler.download(url, depth);
                System.out.println("Downloaded pages:");
                result.getDownloaded().forEach(System.out::println);
                System.out.println("Found error:");
                result.getErrors().forEach((key, value) -> System.out.println(key + " " + value));
            }
        }
    }
}