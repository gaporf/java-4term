package ru.ifmo.rain.akimov.bank;

import java.rmi.RemoteException;
import java.util.Set;

public abstract class CommonPerson implements Person {
    private final String firstName;
    private final String lastName;
    private final String passportId;

    public CommonPerson(final String firstName, final String lastName, final String passportId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passportId = passportId;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getPassportId() {
        return passportId;
    }

    @Override
    public abstract Account getAccount(String accountId) throws RemoteException;

    @Override
    public abstract Set<String> getAccounts() throws RemoteException;

    @Override
    public abstract Account createAccount(String accountId) throws RemoteException;
}
