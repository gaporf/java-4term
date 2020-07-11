package ru.ifmo.rain.akimov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Person extends Remote {
    String getFirstName() throws RemoteException;

    String getLastName() throws RemoteException;

    String getPassportId() throws RemoteException;

    Account getAccount(String accountId) throws RemoteException;

    Set<String> getAccounts() throws RemoteException;

    Account createAccount(String accountId) throws RemoteException;
}
