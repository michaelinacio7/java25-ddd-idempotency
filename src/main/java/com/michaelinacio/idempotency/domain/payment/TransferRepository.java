package com.michaelinacio.idempotency.domain.payment;

import java.util.UUID;

public interface TransferRepository {
    BankTransfer transfer(UUID customerId, UUID sourceAccountId, UUID destinationAccountId,
                          com.michaelinacio.idempotency.domain.account.Money amount, String idempotencyKey,
                          String requestFingerprint);
}