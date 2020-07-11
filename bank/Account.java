package ru.ifmo.rain.akimov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    String getFullId() throws RemoteException;

    String getPassportId() throws RemoteException;

    String getAccountId() throws RemoteException;

    long getAmount() throws RemoteException;

    void changeAmount(long amount) throws RemoteException;
}
