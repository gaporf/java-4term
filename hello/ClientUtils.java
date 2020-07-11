package ru.ifmo.rain.akimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

public class ClientUtils {
    private static int missChars(int start, final String string, final Predicate<Character> predicate) {
        while (start < string.length() && !predicate.test(string.charAt(start))) {
            start++;
        }
        return start;
    }

    private static boolean areDifferent(final String first, final int pos, final String second) {
        return !first.regionMatches(pos, second, 0, second.length());
    }

    public static boolean isCorrect(final String numOfThread, final String numOfRequest, final String received) {
        int pos = missChars(0, received, Character::isDigit);
        if (areDifferent(received, pos, numOfThread)) {
            return false;
        }
        pos += numOfThread.length();
        pos = missChars(pos, received, Character::isDigit);
        if (areDifferent(received, pos, numOfRequest)) {
            return false;
        }
        pos += numOfRequest.length();
        pos = missChars(pos, received, Character::isDigit);
        return pos == received.length();
    }

    public static SocketAddress getSocketAddress(final String host, final int port) {
        final InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (final UnknownHostException e) {
            System.err.println("Hostname is incorrect: " + e.getMessage());
            return null;
        }
        return new InetSocketAddress(inetAddress, port);
    }

    public static void mainFunction(final String[] args, final HelloClient helloClient) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("The arguments have to be not null");
            return;
        }
        if (args.length != 5) {
            System.err.println("It's necessary to put 5 arguments");
            return;
        }
        final String host = args[0];
        final String prefix = args[2];
        try {
            final int port = Integer.parseInt(args[1]);
            final int threads = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);
            helloClient.run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e) {
            System.err.println("Invalid number: " + e.getMessage());
        }
    }
}
