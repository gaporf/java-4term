package ru.ifmo.rain.akimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private ExecutorService handlers;
    private DatagramSocket socket;

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (final IOException e) {
            System.err.println("Could not create socket: " + e.getMessage());
            return;
        }
        handlers = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            handlers.submit(() -> {
                final int bufferSize;
                try {
                    bufferSize = this.socket.getReceiveBufferSize();
                } catch (final SocketException ignored) {
                    return;
                }
                final byte[] buffer = new byte[bufferSize];
                final DatagramPacket packet = new DatagramPacket(buffer, bufferSize);
                while (!this.socket.isClosed()) {
                    try {
                        packet.setData(buffer);
                        this.socket.receive(packet);
                        final String request = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                        System.out.println(request);
                        final byte[] bytes = ("Hello, " + request).getBytes(StandardCharsets.UTF_8);
                        packet.setData(bytes);
                        this.socket.send(packet);
                    } catch (final IOException ignored) {
                    }
                }
            });
        }
    }

    @Override
    public void close() {
        if (!socket.isClosed()) {
            socket.close();
            handlers.shutdownNow();
            try {
                handlers.awaitTermination(100, TimeUnit.SECONDS);
            } catch (final InterruptedException ignored) {
            }
        }
    }

    public static void main(final String[] args) {
        try (final HelloServer helloServer = new HelloUDPServer()) {
            ServerUtils.mainFunction(args, helloServer);
        }
    }
}
