package com.michaelinacio.idempotency.domain.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    @Test
    void createsPaymentWithValidAmount() {
        var payment = Payment.create(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        assertNotNull(payment.id());
        assertEquals(0, payment.amount().compareTo(new BigDecimal("10.00")));
        assertEquals("BRL", payment.currency().getCurrencyCode());
    }

    @Test
    void rejectsZeroOrNegativeAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> Payment.create(BigDecimal.ZERO, Currency.getInstance("BRL")));
        assertThrows(IllegalArgumentException.class,
                () -> Payment.create(new BigDecimal("-1.00"), Currency.getInstance("BRL")));
    }
}