package com.orders.order.model;

public enum OrderStatus {

    CREATED,
    PENDING_PAYMENT,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
