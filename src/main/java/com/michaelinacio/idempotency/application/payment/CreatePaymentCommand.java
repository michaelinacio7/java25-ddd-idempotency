package com.michaelinacio.idempotency.application.payment;

import java.math.BigDecimal;
import java.util.Currency;

public record CreatePaymentCommand(BigDecimal amount, Currency currency) {}