package com.michaelinacio.saga.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface OrderActivities {
    @ActivityMethod void reserveInventory(OrderRequest request);
    @ActivityMethod void releaseInventory(OrderRequest request);
    @ActivityMethod void chargePayment(OrderRequest request);
    @ActivityMethod void refundPayment(OrderRequest request);
    @ActivityMethod void reserveDelivery(OrderRequest request);
    @ActivityMethod void cancelDelivery(OrderRequest request);
}
