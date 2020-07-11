package ru.ifmo.rain.akimov.bank;

import java.util.Objects;

public class CommonAccount implements Account {
    private final String passportId, accountId;
    private long amount;

    public CommonAccount(final String passportId, final String accountId, final long amount) {
        this.passportId = passportId;
        this.accountId = accountId;
        this.amount = amount;
    }

    @Override
    public String getFullId() {
        return passportId + ':' + accountId;
    }

    @Override
    public String getPassportId() {
        return passportId;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public long getAmount() {
        System.out.println("Getting amount of money for account " + getFullId());
        return amount;
    }

    @Override
    public void changeAmount(final long amount) {
        System.out.println("Setting amount of money for account " + getFullId());
        this.amount += amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonAccount)) return false;
        final CommonAccount commonAccount = (CommonAccount) o;
        return amount == commonAccount.amount &&
                passportId.equals(commonAccount.passportId) &&
                accountId.equals(commonAccount.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passportId, accountId, amount);
    }
}
