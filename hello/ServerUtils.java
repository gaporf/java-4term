package ru.ifmo.rain.akimov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class ServerUtils {
    public static void mainFunction(final String[] args, final HelloServer helloServer) {
        if (args != null && args.length == 2 && Arrays.stream(args).noneMatch(Objects::isNull)) {
            try {
                final int port = Integer.parseInt(args[0]);
                final int threads = Integer.parseInt(args[1]);
                helloServer.start(port, threads);
                final Scanner scanner = new Scanner(System.in);
                while (true) {
                    System.out.println("Print \"close\" to close server");
                    final String string = scanner.nextLine();
                    if (string.equals("close")) {
                        break;
                    }
                }
            } catch (final NumberFormatException e) {
                System.err.println("Invalid number: " + e.getMessage());
            }
        } else {
            System.err.println("The arguments have to be not null");
        }
    }
}
