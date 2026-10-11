package com.michaelinacio.idempotency.domain.payment;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public final class Payment {
    private final UUID id;
    private final BigDecimal amount;
    private final Currency currency;

    private Payment(UUID id, BigDecimal amount, Currency currency) {
        this.id = Objects.requireNonNull(id);
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
    }

    public static Payment create(BigDecimal amount, Currency currency) {
        return new Payment(UUID.randomUUID(), amount, currency);
    }

    public UUID id() { return id; }
    public BigDecimal amount() { return amount; }
    public Currency currency() { return currency; }
}