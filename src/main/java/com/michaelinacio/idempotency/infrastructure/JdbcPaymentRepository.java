package com.michaelinacio.idempotency.infrastructure;

import com.michaelinacio.idempotency.domain.payment.Payment;
import com.michaelinacio.idempotency.domain.payment.PaymentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {
    private final JdbcTemplate jdbc;

    public JdbcPaymentRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Payment save(Payment payment) {
        jdbc.update("INSERT INTO payments(id, amount, currency) VALUES (?, ?, ?)",
                payment.id(), payment.amount(), payment.currency().getCurrencyCode());
        return payment;
    }
}