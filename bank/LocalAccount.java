package ru.ifmo.rain.akimov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount extends CommonAccount implements Serializable {
    public LocalAccount(final Account account) throws RemoteException {
        this(account.getPassportId(), account.getAccountId(), account.getAmount());
    }

    public LocalAccount(final String passportId, final String accountId, final long amount) {
        super(passportId, accountId, amount);
    }

    public LocalAccount(final String passportId, final String accountId) {
        super(passportId, accountId, 0);
    }
}
