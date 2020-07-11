package ru.ifmo.rain.akimov.bank;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server {
    private static int PORT = 32391;

    public static void main(final String[] args) {
        try {
            final Bank bank = new RemoteBank(PORT);
            UnicastRemoteObject.exportObject(bank, PORT);
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind("//localhost/bank", bank);
        } catch (final RemoteException e) {
            System.err.println("Cannot export object: " + e.getMessage());
        }
        System.out.println("Server started");
    }
}
