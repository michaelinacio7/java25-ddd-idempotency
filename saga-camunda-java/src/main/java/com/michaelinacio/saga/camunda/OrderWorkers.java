package com.michaelinacio.saga.camunda;

import io.camunda.spring.client.annotation.JobWorker;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderWorkers {
    @JobWorker(type = "reserve-inventory")
    public Map<String, Object> reserveInventory(Map<String, Object> variables) {
        // Replace with an idempotent inventory adapter; use orderId as its deduplication key.
        return Map.of("inventoryReserved", true);
    }

    @JobWorker(type = "release-inventory")
    public Map<String, Object> releaseInventory(Map<String, Object> variables) {
        return Map.of("inventoryCompensated", true);
    }

    @JobWorker(type = "charge-payment")
    public Map<String, Object> chargePayment(Map<String, Object> variables) {
        // Sandbox only: no real payment provider is called.
        return Map.of("paymentCharged", true);
    }

    @JobWorker(type = "refund-payment")
    public Map<String, Object> refundPayment(Map<String, Object> variables) {
        return Map.of("paymentCompensated", true);
    }

    @JobWorker(type = "reserve-delivery")
    public Map<String, Object> reserveDelivery(Map<String, Object> variables) {
        return Map.of("deliveryReserved", true);
    }

    @JobWorker(type = "cancel-delivery")
    public Map<String, Object> cancelDelivery(Map<String, Object> variables) {
        return Map.of("deliveryCompensated", true);
    }
}
