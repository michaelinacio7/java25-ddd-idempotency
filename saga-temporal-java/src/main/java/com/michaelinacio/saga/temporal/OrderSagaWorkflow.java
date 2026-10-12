package com.michaelinacio.saga.temporal;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.Saga;
import io.temporal.activity.ActivityOptions;

import java.time.Duration;

public class OrderSagaWorkflow implements OrderSaga {
    private final OrderActivities activities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(20))
                    .setScheduleToCloseTimeout(Duration.ofMinutes(2))
                    .build());

    @Override
    public String placeOrder(OrderRequest request) {
        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        try {
            // Register compensation before executing an operation that may fail
            // after the remote side effect has already happened.
            saga.addCompensation(activities::releaseInventory, request);
            activities.reserveInventory(request);

            saga.addCompensation(activities::refundPayment, request);
            activities.chargePayment(request);

            saga.addCompensation(activities::cancelDelivery, request);
            activities.reserveDelivery(request);

            return "CONFIRMED:" + request.orderId();
        } catch (RuntimeException failure) {
            saga.compensate();
            throw failure;
        }
    }
}
