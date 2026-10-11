package com.michaelinacio.idempotency.domain.payment;

import com.michaelinacio.idempotency.domain.account.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BankTransfer(UUID id, UUID sourceAccountId, UUID destinationAccountId,
                           Money amount, Status status, Instant createdAt) {
    public enum Status { POSTED, REJECTED }

    public BankTransfer {
        Objects.requireNonNull(id);
        Objects.requireNonNull(sourceAccountId);
        Objects.requireNonNull(destinationAccountId);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        if (sourceAccountId.equals(destinationAccountId)) throw new IllegalArgumentException("Source and destination accounts must differ");
        if (!amount.isPositive()) throw new IllegalArgumentException("Transfer amount must be positive");
    }

    public static BankTransfer posted(UUID source, UUID destination, Money amount) {
        return new BankTransfer(UUID.randomUUID(), source, destination, amount, Status.POSTED, Instant.now());
    }
}