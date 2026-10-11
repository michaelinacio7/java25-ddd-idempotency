package com.michaelinacio.idempotency.domain.payment;

public interface PaymentRepository {
    Payment save(Payment payment);
}