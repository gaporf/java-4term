package ru.ifmo.rain.akimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer implements HelloServer {
    private final Queue<ByteBuffer> dstBuffers = new ConcurrentLinkedQueue<>();
    private final Queue<Pair> responds = new ConcurrentLinkedQueue<>();

    private static class Pair {
        public Pair(final String message, final SocketAddress address) {
            this.message = message;
            this.address = address;
        }

        public final String message;
        public final SocketAddress address;
    }

    private static final int TIMEOUT = 1000;
    private ExecutorService handlers;

    synchronized private void check(final SelectionKey key, final Selector selector) {
        key.interestOps((dstBuffers.isEmpty() ? 0 : SelectionKey.OP_READ) | (responds.isEmpty() ? 0 : SelectionKey.OP_WRITE));
        selector.wakeup();
    }

    @Override
    public void start(final int port, final int threads) {
        handlers = Executors.newFixedThreadPool(threads + 1);
        final CountDownLatch latch = new CountDownLatch(1);
        handlers.submit(() -> {
            try (final Selector selector = Selector.open()) {
                try (final DatagramChannel channel = DatagramChannel.open()) {
                    channel.bind(new InetSocketAddress(port));
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ);
                    final int bufferSize = channel.socket().getReceiveBufferSize();
                    for (int thread = 0; thread < threads; thread++) {
                        dstBuffers.add(ByteBuffer.allocate(bufferSize));
                    }
                    latch.countDown();
                    while (!handlers.isShutdown()) {
                        try {
                            selector.select();
                            for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                                final SelectionKey key = i.next();
                                try {
                                    if (key.isReadable() && key.isValid()) {
                                        final ByteBuffer dst = dstBuffers.poll();
                                        if (dst == null) {
                                            check(key, selector);
                                            continue;
                                        }
                                        dst.clear();
                                        final SocketAddress clientAddress = channel.receive(dst);
                                        handlers.submit(() -> {
                                            dst.flip();
                                            final String request = new String(dst.array(), dst.arrayOffset(), dst.limit(), StandardCharsets.UTF_8);
//                                            System.out.println("get: " + request);
                                            responds.add(new Pair("Hello, " + request, clientAddress));
                                            dstBuffers.add(dst);
                                            check(key, selector);
                                        });
                                    }
                                    if (key.isWritable() && key.isValid()) {
                                        final Pair pair = responds.poll();
                                        if (pair == null) {
                                            check(key, selector);
                                            continue;
                                        }
                                        channel.send(ByteBuffer.wrap(pair.message.getBytes(StandardCharsets.UTF_8)), pair.address);
                                    }
                                } finally {
                                    i.remove();
                                }
                            }
                        } catch (final IOException ignored) {
                        }
                    }
                }
            } catch (final IOException ignored) {
            }
        });
        try {
            latch.await();
        } catch (final InterruptedException ignored) {
        }
    }

    @Override
    public void close() {
        handlers.shutdownNow();
        try {
            handlers.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
        } catch (final InterruptedException ignored) {
        }
    }

    public static void main(final String[] args) {
        try (final HelloServer helloServer = new HelloUDPNonblockingServer()) {
            ServerUtils.mainFunction(args, helloServer);
        }
    }
}
