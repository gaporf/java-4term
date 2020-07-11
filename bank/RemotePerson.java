package ru.ifmo.rain.akimov.bank;

import java.rmi.RemoteException;
import java.util.Set;

public class RemotePerson extends CommonPerson {
    private final Bank bank;

    public RemotePerson(final String firstName, final String lastName, final String passportId, final Bank bank) {
        super(firstName, lastName, passportId);
        this.bank = bank;
    }

    @Override
    public Account getAccount(final String accountId) throws RemoteException {
        return bank.getAccount(getPassportId(), accountId);
    }

    @Override
    public Set<String> getAccounts() throws RemoteException {
        return bank.getAccounts(getPassportId());
    }

    @Override
    public Account createAccount(final String accountId) throws RemoteException {
        return bank.createAccount(getPassportId(), accountId);
    }
}
