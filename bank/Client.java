package ru.ifmo.rain.akimov.bank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.Objects;

public class Client {

    public static void main(final String[] args) throws RemoteException {
        if (args == null || args.length != 5 && Arrays.stream(args).anyMatch(Objects::nonNull)) {
            System.err.println("Invalid arguments, it's necessary to put 5 not-null arguments");
            return;
        }
        try {
            final String firstName = args[0];
            final String lastName = args[1];
            final String passportId = args[2];
            final String accountId = args[3];
            final int amount = Integer.parseInt(args[4]);
            final int PORT = 32391;
            Registry registry = LocateRegistry.getRegistry(PORT);
            final Bank bank = (Bank) registry.lookup("//localhost/bank");
            if (bank.createPerson(firstName, lastName, passportId) == null) {
                System.err.println("Incorrect data");
                return;
            }
            bank.createAccount(passportId, accountId).changeAmount(amount);
        } catch (final NumberFormatException e) {
            System.err.println("Invalid number: " + e.getMessage());
        } catch (final NotBoundException ignored) {
            System.err.println("Bank is not bound");
        }
    }
}
