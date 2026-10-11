package com.michaelinacio.idempotency.domain.account;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Currency;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test void addsMoneyInSameCurrency() {
        var a = new Money(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        var b = new Money(new BigDecimal("2.50"), Currency.getInstance("BRL"));
        assertEquals(new BigDecimal("12.50"), a.add(b).amount());
    }

    @Test void rejectsDifferentCurrencies() {
        var brl = new Money(new BigDecimal("10.00"), Currency.getInstance("BRL"));
        var usd = new Money(new BigDecimal("2.00"), Currency.getInstance("USD"));
        assertThrows(IllegalArgumentException.class, () -> brl.add(usd));
    }

    @Test void rejectsPrecisionLoss() {
        assertThrows(ArithmeticException.class,
                () -> new Money(new BigDecimal("10.001"), Currency.getInstance("BRL")));
    }
}