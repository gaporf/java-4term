package ru.ifmo.rain.akimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static ru.ifmo.rain.akimov.hello.ClientUtils.isCorrect;

public class HelloUDPClient implements HelloClient {
    private void sendRequest(final String prefix, final int thread, final int requests, final SocketAddress serverSocket) {
        try (final DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(100);
            final int bufferSize = datagramSocket.getReceiveBufferSize();
            final DatagramPacket receivePacket = new DatagramPacket(new byte[bufferSize], bufferSize);
            for (int j = 0; j < requests; j++) {
                final byte[] bytes = (prefix + thread + '_' + j).getBytes(StandardCharsets.UTF_8);
                final DatagramPacket sendPacket = new DatagramPacket(bytes, bytes.length, serverSocket);
                while (!datagramSocket.isClosed()) {
                    try {
                        datagramSocket.send(sendPacket);
                        datagramSocket.receive(receivePacket);
                        final String received = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), StandardCharsets.UTF_8);
                        if (isCorrect(Integer.toString(thread), Integer.toString(j), received)) {
                            System.out.println("received: " + received);
                            break;
                        }
                    } catch (final IOException ignored) {
                    }
                }
            }
        } catch (final SocketException e) {
            System.err.println("Could not create socket: " + e.getMessage());
        }
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final SocketAddress server = ClientUtils.getSocketAddress(host, port);
        if (server == null) {
            return;
        }
        final ExecutorService runningThreads = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int finalI = i;
            runningThreads.submit(() -> sendRequest(prefix, finalI, requests, server));
        }
        try {
            runningThreads.shutdown();
            runningThreads.awaitTermination(100, TimeUnit.MINUTES);
        } catch (final InterruptedException ignored) {
        }
        System.out.println(System.lineSeparator() + "All requests are sent!" + System.lineSeparator());
    }

    public static void main(final String[] args) {
        ClientUtils.mainFunction(args, new HelloUDPClient());
    }
}