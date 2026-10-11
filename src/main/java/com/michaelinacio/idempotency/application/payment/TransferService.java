package com.michaelinacio.idempotency.application.payment;

import com.michaelinacio.idempotency.domain.account.Money;
import com.michaelinacio.idempotency.domain.payment.BankTransfer;
import com.michaelinacio.idempotency.domain.payment.TransferRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class TransferService {
    private final TransferRepository transfers;

    public TransferService(TransferRepository transfers) {
        this.transfers = transfers;
    }

    public BankTransfer transfer(TransferCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.amount() == null || command.currency() == null || command.currency().isBlank()) {
            throw new IllegalArgumentException("AMOUNT_AND_CURRENCY_REQUIRED");
        }
        Money amount = new Money(command.amount(), java.util.Currency.getInstance(command.currency()));
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        String fingerprint = fingerprint(command, amount);
        return transfers.transfer(command.customerId(), command.sourceAccountId(),
                command.destinationAccountId(), amount, command.idempotencyKey(), fingerprint);
    }

    private String fingerprint(TransferCommand command, Money amount) {
        // Normalize the amount and currency before hashing so equivalent decimal
        // representations produce the same request fingerprint.
        String canonical = command.sourceAccountId() + "\n"
                + command.destinationAccountId() + "\n"
                + amount.amount().toPlainString() + "\n"
                + amount.currency().getCurrencyCode();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
