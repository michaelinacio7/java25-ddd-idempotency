package com.michaelinacio.saga.temporal;

import java.math.BigDecimal;

public record OrderRequest(String orderId, String customerId, String sku, int quantity,
                           BigDecimal amount, String currency) {
}
