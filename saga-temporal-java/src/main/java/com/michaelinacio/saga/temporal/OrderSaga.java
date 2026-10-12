package com.michaelinacio.saga.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderSaga {
    @WorkflowMethod
    String placeOrder(OrderRequest request);
}
