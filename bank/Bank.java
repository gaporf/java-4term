package ru.ifmo.rain.akimov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Bank extends Remote {
    Person createPerson(String firstName, String lastName, String passportId) throws RemoteException;

    LocalPerson getLocalPerson(String passportId) throws RemoteException;

    RemotePerson getRemotePerson(String passportId) throws RemoteException;

    boolean checkPersonByPassport(String passportId, String firstName, String lastName) throws RemoteException;

    Account createAccount(String passportId, String accountId) throws RemoteException;

    Account getAccount(String passportId, String accountId) throws RemoteException;

    Set<String> getAccounts(String passportId) throws RemoteException;
}
