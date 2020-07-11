package ru.ifmo.rain.akimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class HelloUDPNonblockingClient implements HelloClient {
    private static final int TIMEOUT = 100;

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress serverSocket = ClientUtils.getSocketAddress(host, port);
        if (serverSocket == null) {
            return;
        }
        final List<DatagramChannel> channelList = new ArrayList<>();
        try (final Selector selector = Selector.open()) {
            int necessaryToSend = threads * requests;
            final int[] numRequests = new int[threads];
            final ByteBuffer[] dstBuffers = new ByteBuffer[threads];
            for (int i = 0; i < threads; i++) {
                numRequests[i] = 0;
                final DatagramChannel channel = DatagramChannel.open();
                dstBuffers[i] = ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
                channel.configureBlocking(false);
                channelList.add(channel);
                channel.register(selector, SelectionKey.OP_READ, i);
            }
            while (necessaryToSend > 0) {
                if (selector.select(TIMEOUT) == 0) {
                    final Set<SelectionKey> keys = selector.keys();
                    for (final SelectionKey key : keys) {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                } else {
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        final int thread = (int) key.attachment();
                        final DatagramChannel channel = (DatagramChannel) key.channel();
                        try {
                            if (key.isReadable()) {
                                final ByteBuffer dst = dstBuffers[thread];
                                dst.clear();
                                channel.receive(dst);
                                dst.flip();

                                key.interestOps(SelectionKey.OP_WRITE);
                                if (ClientUtils.isCorrect(Integer.toString(thread), Integer.toString(numRequests[thread]),
                                        new String(dst.array(), dst.arrayOffset(), dst.limit(), StandardCharsets.UTF_8))) {
                                    necessaryToSend--;
                                    numRequests[thread]++;
                                    if (numRequests[thread] == requests) {
                                        key.cancel();
                                    }
                                }
                            } else if (key.isWritable()) {
                                channel.send(ByteBuffer.wrap((prefix + thread + "_" + numRequests[thread]).getBytes(StandardCharsets.UTF_8)), serverSocket);
                                key.interestOps(SelectionKey.OP_READ);
                            }
                        } finally {
                            i.remove();
                        }
                    }
                }
            }
        } catch (final IOException e) {
            System.err.println("Could not send request: " + e.getMessage());
        } finally {
            channelList.forEach(channel -> {
                try {
                    channel.close();
                } catch (final IOException ignored) {
                }
            });
        }
    }

    public static void main(final String[] args) {
        ClientUtils.mainFunction(args, new HelloUDPNonblockingClient());
    }
}
