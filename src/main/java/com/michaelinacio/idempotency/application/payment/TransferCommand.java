package com.michaelinacio.idempotency.application.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(UUID customerId, UUID sourceAccountId, UUID destinationAccountId,
                              BigDecimal amount, String currency, String idempotencyKey) {
}
