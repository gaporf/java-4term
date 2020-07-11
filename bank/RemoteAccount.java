package ru.ifmo.rain.akimov.bank;

public class RemoteAccount extends CommonAccount {
    public RemoteAccount(final String passportId, final String accountId) {
        super(passportId, accountId, 0);
    }
}
