package ru.ifmo.rain.akimov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalPerson extends CommonPerson implements Serializable {
    Map<String, Account> accounts;

    public LocalPerson(final String firstName, final String lastName, final String passportId, final Bank bank) throws RemoteException {
        super(firstName, lastName, passportId);
        accounts = new HashMap<>();
        for (final String accountId : bank.getAccounts(passportId)) {
            accounts.put(accountId, new LocalAccount(bank.getAccount(getPassportId(), accountId)));
        }
    }

    @Override
    public Account getAccount(final String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public Set<String> getAccounts() {
        return accounts.keySet();
    }

    @Override
    public Account createAccount(final String accountId) {
        accounts.putIfAbsent(accountId, new LocalAccount(getPassportId(), accountId));
        return accounts.get(accountId);
    }
}
