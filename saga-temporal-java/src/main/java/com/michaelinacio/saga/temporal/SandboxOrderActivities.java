package com.michaelinacio.saga.temporal;

import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SandboxOrderActivities implements OrderActivities {
    private static final Logger log = LoggerFactory.getLogger(SandboxOrderActivities.class);

    @Override public void reserveInventory(OrderRequest request) { log.info("reserve inventory orderId={}", request.orderId()); }
    @Override public void releaseInventory(OrderRequest request) { log.info("compensate inventory orderId={}", request.orderId()); }
    @Override public void chargePayment(OrderRequest request) { log.info("sandbox charge orderId={} amount={} {}", request.orderId(), request.amount(), request.currency()); }
    @Override public void refundPayment(OrderRequest request) { log.info("sandbox refund orderId={}", request.orderId()); }
    @Override public void reserveDelivery(OrderRequest request) {
        if ("FAIL_DELIVERY".equals(request.sku())) {
            throw Activity.wrap(new IllegalStateException("Simulated delivery failure"));
        }
        log.info("reserve delivery orderId={}", request.orderId());
    }
    @Override public void cancelDelivery(OrderRequest request) { log.info("compensate delivery orderId={}", request.orderId()); }
}
