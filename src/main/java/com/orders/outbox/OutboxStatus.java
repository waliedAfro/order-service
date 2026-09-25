package com.orders.outbox;

public enum OutboxStatus {

    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
