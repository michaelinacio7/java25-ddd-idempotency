package com.michaelinacio.idempotency.domain.account;

import java.util.Objects;
import java.util.UUID;

public final class Account {
    public enum Status { ACTIVE, FROZEN, CLOSED }

    private final UUID id;
    private final UUID customerId;
    private final Money balance;
    private final Status status;

    public Account(UUID id, UUID customerId, Money balance, Status status) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.balance = Objects.requireNonNull(balance);
        this.status = Objects.requireNonNull(status);
        if (balance.isNegative()) throw new IllegalArgumentException("Account balance cannot be negative");
    }

    public void assertCanDebit(Money amount) {
        if (status != Status.ACTIVE) throw new IllegalStateException("ACCOUNT_NOT_ACTIVE");
        if (!balance.currency().equals(amount.currency())) throw new IllegalArgumentException("CURRENCY_MISMATCH");
        if (!amount.isPositive()) throw new IllegalArgumentException("INVALID_AMOUNT");
        if (balance.compareTo(amount) < 0) throw new IllegalStateException("INSUFFICIENT_FUNDS");
    }

    public UUID id() { return id; }
    public UUID customerId() { return customerId; }
    public Money balance() { return balance; }
    public Status status() { return status; }
}