package ru.ifmo.rain.akimov.bank;

import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteBank implements Bank {
    private final int port;
    private final Map<String, Person> persons = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Account>> accounts = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Person createPerson(final String firstName, final String lastName, final String passportId) throws RemoteException {
        final Person person = new RemotePerson(firstName, lastName, passportId, this);
        if (persons.putIfAbsent(passportId, person) == null) {
            System.out.println("Creating person with passport " + passportId);
            accounts.put(passportId, new ConcurrentHashMap<>());
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return checkPersonByPassport(passportId, firstName, lastName) ? getPerson(passportId, RemotePerson.class) : null;
        }
    }

    private Person getPerson(final String passportId, final Class<? extends Person> typePerson) throws RemoteException {
        System.out.println("Retrieving person with passport " + passportId);
        final Person person = persons.get(passportId);
        if (person == null) {
            System.err.println("Person does not exist");
            return null;
        } else if (typePerson == RemotePerson.class) {
            return person;
        } else {
            final String firstName = person.getFirstName();
            final String lastName = person.getLastName();
            return new LocalPerson(firstName, lastName, passportId, this)   ;
        }
    }

    @Override
    public LocalPerson getLocalPerson(final String passportId) throws RemoteException {
        return (LocalPerson) getPerson(passportId, LocalPerson.class);
    }

    @Override
    public RemotePerson getRemotePerson(final String passportId) throws RemoteException {
        return (RemotePerson) getPerson(passportId, RemotePerson.class);
    }

    @Override
    public boolean checkPersonByPassport(final String passportId, final String firstName, final String lastName) throws RemoteException {
        final Person person = persons.get(passportId);
        return person != null && person.getFirstName().equals(firstName) && person.getLastName().equals(lastName);
    }

    @Override
    public Account createAccount(final String passportId, final String accountId) throws RemoteException {
        final Person person = persons.get(passportId);
        if (person == null) {
            System.err.println("Cannot create account for not existing person");
            return null;
        }
        final Map<String, Account> personAccounts = accounts.get(passportId);
        final Account account = new RemoteAccount(passportId, accountId);
        if (personAccounts.putIfAbsent(accountId, account) == null) {
            System.out.println("Creating account " + passportId + ':' + accountId);
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(passportId, accountId);
        }
    }

    @Override
    public Account getAccount(final String passportId, final String accountId) {
        System.out.println("Retrieving account with passport " + passportId);
        return accounts.getOrDefault(passportId, Map.of()).get(accountId);
    }

    @Override
    public Set<String> getAccounts(final String passportId) {
        return Set.copyOf(accounts.getOrDefault(passportId, new HashMap<>()).keySet());
    }
}
